/*
 * Copyright (c) 2026 ChronoClass. All rights reserved.
 * This software is submitted for evaluation purposes only.
 * Unauthorized commercial use, reproduction, or distribution is prohibited.
 */
package com.chronoclass.service;

import com.chronoclass.dto.request.BookOfferingRequest;
import com.chronoclass.dto.response.BookingResponse;
import com.chronoclass.dto.response.OfferingResponse;
import com.chronoclass.dto.response.SessionResponse;
import com.chronoclass.entity.Booking;
import com.chronoclass.entity.Offering;
import com.chronoclass.entity.Session;
import com.chronoclass.exception.ConcurrentBookingException;
import com.chronoclass.exception.ResourceNotFoundException;
import com.chronoclass.exception.TimeConflictException;
import com.chronoclass.repository.BookingRepository;
import com.chronoclass.repository.OfferingRepository;
import com.chronoclass.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service handling all parent-facing operations:
 * viewing available offerings, booking, and viewing bookings.
 *
 * <p><b>Concurrency Strategy:</b></p>
 * <ul>
 *   <li>Pessimistic locking (SELECT ... FOR UPDATE) is used during the booking flow
 *       to serialize access to the offering and the parent's existing booked sessions.</li>
 *   <li>This prevents two concurrent booking requests from creating overlapping bookings
 *       for the same parent, and prevents overbooking the offering capacity.</li>
 * </ul>
 *
 * <p><b>Timezone Strategy:</b></p>
 * <ul>
 *   <li>All session times are stored in UTC in the database.</li>
 *   <li>When parents view offerings, times are converted to their local timezone.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParentService {

    private final OfferingRepository offeringRepository;
    private final SessionRepository sessionRepository;
    private final BookingRepository bookingRepository;

    /**
     * Returns all available (ACTIVE) offerings with session times converted to
     * the parent's timezone.
     *
     * @param timezone IANA timezone ID (e.g., "Asia/Kolkata")
     * @return list of available offerings
     */
    @Transactional(readOnly = true)
    public List<OfferingResponse> getAvailableOfferings(String timezone) {
        ZoneId displayZone = validateTimezone(timezone);
        log.debug("Fetching available offerings for timezone: {}", displayZone);

        List<Offering> offerings = offeringRepository.findAllActiveWithSessions();

        return offerings.stream()
                .filter(Offering::hasCapacity)
                .map(o -> toOfferingResponse(o, displayZone))
                .collect(Collectors.toList());
    }

    /**
     * Books an offering for a parent.
     *
     * <p><b>Concurrency-safe booking flow:</b></p>
     * <ol>
     *   <li>Acquire pessimistic write lock on the target offering</li>
     *   <li>Verify the parent hasn't already booked this offering</li>
     *   <li>Verify the offering has available capacity</li>
     *   <li>Acquire pessimistic write lock on the parent's existing booked sessions</li>
     *   <li>Lock the target offering's sessions</li>
     *   <li>Check for time conflicts between target sessions and already-booked sessions</li>
     *   <li>If no conflicts: create booking, increment enrolled count</li>
     *   <li>If conflicts: throw TimeConflictException with details</li>
     * </ol>
     *
     * @param parentId the parent's ID
     * @param request  the booking request
     * @return booking confirmation
     */
    @Transactional
    public BookingResponse bookOffering(Long parentId, BookOfferingRequest request) {
        log.info("Parent {} attempting to book offering {}", parentId, request.getOfferingId());

        ZoneId parentZone = validateTimezone(request.getParentTimezone());

        // STEP 1: Acquire pessimistic write lock on the offering
        Offering offering = offeringRepository.findByIdForUpdate(request.getOfferingId())
                .orElseThrow(() -> new ResourceNotFoundException("Offering", request.getOfferingId()));

        // STEP 2: Check if parent already booked this offering
        if (bookingRepository.existsByParentIdAndOfferingId(parentId, offering.getId())) {
            throw new ConcurrentBookingException(
                    "You have already booked this offering: " + offering.getTitle());
        }

        // STEP 3: Check offering capacity
        if (!offering.hasCapacity()) {
            throw new ConcurrentBookingException(
                    "Offering '" + offering.getTitle() + "' is fully booked. " +
                    "Maximum capacity: " + offering.getMaxStudents());
        }

        // STEP 4: Verify offering is active
        if (offering.getStatus() != Offering.OfferingStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "Cannot book offering '" + offering.getTitle() +
                    "' — status is: " + offering.getStatus());
        }

        // STEP 5: Acquire pessimistic lock on target offering's sessions
        List<Session> targetSessions = sessionRepository.findByOfferingIdForUpdate(offering.getId());

        if (targetSessions.isEmpty()) {
            throw new IllegalArgumentException(
                    "Cannot book offering '" + offering.getTitle() +
                    "' — it has no sessions scheduled yet.");
        }

        // STEP 6: Acquire pessimistic lock on parent's already-booked sessions
        List<Session> bookedSessions = sessionRepository
                .findBookedSessionsByParentIdForUpdate(parentId);

        // STEP 7: Check for time conflicts
        List<String> conflicts = detectTimeConflicts(targetSessions, bookedSessions, parentZone);

        if (!conflicts.isEmpty()) {
            log.warn("Time conflict detected for parent {} — {} conflicts found",
                    parentId, conflicts.size());
            throw new TimeConflictException(
                    "Cannot book offering '" + offering.getTitle() +
                    "' — " + conflicts.size() +
                    " session(s) overlap with your existing bookings.",
                    conflicts);
        }

        // STEP 8: All clear — create the booking
        Booking booking = Booking.builder()
                .offering(offering)
                .parentId(parentId)
                .parentTimezone(parentZone.getId())
                .status(Booking.BookingStatus.CONFIRMED)
                .build();

        booking = bookingRepository.save(booking);

        // STEP 9: Increment enrolled count (protected by pessimistic lock)
        offering.incrementEnrolledCount();
        offeringRepository.save(offering);

        log.info("Booking confirmed — id: {}, parent: {}, offering: '{}' ({})",
                booking.getId(), parentId, offering.getTitle(), offering.getId());

        // Reload offering with course info for response
        Offering fullOffering = offeringRepository.findByIdWithSessions(offering.getId())
                .orElse(offering);

        return toBookingResponse(booking, fullOffering, parentZone);
    }

    /**
     * Returns all bookings for a parent with session times in the parent's timezone.
     *
     * @param parentId the parent's ID
     * @param timezone optional override timezone (defaults to booking's stored timezone)
     * @return list of bookings
     */
    @Transactional(readOnly = true)
    public List<BookingResponse> getParentBookings(Long parentId, String timezone) {
        log.debug("Fetching bookings for parent {}", parentId);

        List<Booking> bookings = bookingRepository.findByParentIdWithOfferings(parentId);
        ZoneId overrideZone = (timezone != null && !timezone.isBlank())
                ? validateTimezone(timezone) : null;

        return bookings.stream()
                .map(b -> {
                    ZoneId displayZone = overrideZone != null
                            ? overrideZone : ZoneId.of(b.getParentTimezone());
                    return toBookingResponse(b, b.getOffering(), displayZone);
                })
                .collect(Collectors.toList());
    }

    // ======================== Conflict Detection ========================

    /**
     * Detects time conflicts between target sessions (offering being booked)
     * and already-booked sessions.
     *
     * <p>Two sessions conflict if one starts before the other ends
     * AND ends after the other starts.</p>
     *
     * @param targetSessions  sessions of the offering being booked
     * @param bookedSessions  sessions already booked by the parent
     * @param displayZone     timezone for human-readable conflict descriptions
     * @return list of conflict descriptions (empty if no conflicts)
     */
    private List<String> detectTimeConflicts(
            List<Session> targetSessions,
            List<Session> bookedSessions,
            ZoneId displayZone) {

        List<String> conflicts = new ArrayList<>();

        for (Session target : targetSessions) {
            for (Session booked : bookedSessions) {
                if (target.overlapsWith(booked)) {
                    String conflict = String.format(
                            "Session [%s — %s] conflicts with already-booked session [%s — %s]",
                            target.getStartTime().atZone(displayZone).toLocalDateTime(),
                            target.getEndTime().atZone(displayZone).toLocalDateTime(),
                            booked.getStartTime().atZone(displayZone).toLocalDateTime(),
                            booked.getEndTime().atZone(displayZone).toLocalDateTime()
                    );
                    conflicts.add(conflict);
                }
            }
        }

        return conflicts;
    }

    // ======================== DTO Conversion ========================

    private OfferingResponse toOfferingResponse(Offering offering, ZoneId displayZone) {
        List<SessionResponse> sessionResponses = offering.getSessions().stream()
                .map(s -> toSessionResponse(s, displayZone))
                .sorted((a, b) -> a.getStartTime().compareTo(b.getStartTime()))
                .collect(Collectors.toList());

        return OfferingResponse.builder()
                .id(offering.getId())
                .courseName(offering.getCourse().getName())
                .courseDescription(offering.getCourse().getDescription())
                .title(offering.getTitle())
                .teacherId(offering.getTeacherId())
                .teacherTimezone(offering.getTeacherTimezone())
                .maxStudents(offering.getMaxStudents())
                .enrolledCount(offering.getEnrolledCount())
                .availableSpots(offering.getMaxStudents() - offering.getEnrolledCount())
                .status(offering.getStatus().name())
                .displayTimezone(displayZone.getId())
                .sessions(sessionResponses)
                .createdAt(offering.getCreatedAt().atZone(displayZone))
                .build();
    }

    private SessionResponse toSessionResponse(Session session, ZoneId displayZone) {
        ZonedDateTime start = session.getStartTime().atZone(displayZone);
        ZonedDateTime end = session.getEndTime().atZone(displayZone);

        return SessionResponse.builder()
                .id(session.getId())
                .offeringId(session.getOffering().getId())
                .startTime(start)
                .endTime(end)
                .durationMinutes(Duration.between(start, end).toMinutes())
                .build();
    }

    private BookingResponse toBookingResponse(Booking booking, Offering offering, ZoneId displayZone) {
        return BookingResponse.builder()
                .id(booking.getId())
                .parentId(booking.getParentId())
                .parentTimezone(booking.getParentTimezone())
                .status(booking.getStatus().name())
                .offering(toOfferingResponse(offering, displayZone))
                .bookedAt(booking.getCreatedAt().atZone(displayZone))
                .build();
    }

    private ZoneId validateTimezone(String timezone) {
        try {
            return ZoneId.of(timezone);
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("Invalid timezone: '" + timezone +
                    "'. Use IANA timezone IDs like 'America/New_York' or 'Asia/Kolkata'.");
        }
    }
}

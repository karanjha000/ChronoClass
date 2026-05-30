/*
 * Copyright (c) 2026 ChronoClass. All rights reserved.
 * This software is submitted for evaluation purposes only.
 * Unauthorized commercial use, reproduction, or distribution is prohibited.
 */
package com.chronoclass.service;

import com.chronoclass.dto.request.AddSessionsRequest;
import com.chronoclass.dto.request.CreateOfferingRequest;
import com.chronoclass.dto.response.OfferingResponse;
import com.chronoclass.dto.response.SessionResponse;
import com.chronoclass.entity.Course;
import com.chronoclass.entity.Offering;
import com.chronoclass.entity.Session;
import com.chronoclass.exception.ResourceNotFoundException;
import com.chronoclass.repository.CourseRepository;
import com.chronoclass.repository.OfferingRepository;
import com.chronoclass.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service handling all teacher-facing operations:
 * creating offerings, adding sessions, and viewing offerings.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherService {

    private final CourseRepository courseRepository;
    private final OfferingRepository offeringRepository;
    private final SessionRepository sessionRepository;

    /**
     * Creates a new offering for a teacher.
     * If the course doesn't exist, it creates the course first.
     *
     * @param teacherId the teacher's ID
     * @param request   the offering creation request
     * @return the created offering response
     */
    @Transactional
    public OfferingResponse createOffering(Long teacherId, CreateOfferingRequest request) {
        log.info("Creating offering for teacher {} — course: '{}', title: '{}'",
                teacherId, request.getCourseName(), request.getTitle());

        // Validate timezone
        ZoneId teacherZone = validateTimezone(request.getTeacherTimezone());

        // Find or create the course
        Course course = courseRepository.findByNameIgnoreCase(request.getCourseName())
                .orElseGet(() -> {
                    log.info("Creating new course: '{}'", request.getCourseName());
                    Course newCourse = Course.builder()
                            .name(request.getCourseName())
                            .description(request.getCourseDescription())
                            .build();
                    return courseRepository.save(newCourse);
                });

        // Create the offering
        Offering offering = Offering.builder()
                .course(course)
                .teacherId(teacherId)
                .title(request.getTitle())
                .teacherTimezone(teacherZone.getId())
                .maxStudents(request.getMaxStudents() != null ? request.getMaxStudents() : 30)
                .build();

        offering = offeringRepository.save(offering);
        log.info("Created offering with id: {}", offering.getId());

        return toOfferingResponse(offering, teacherZone);
    }

    /**
     * Adds sessions to an existing offering.
     * Session times are provided in the teacher's timezone and converted to UTC for storage.
     *
     * @param offeringId the offering ID
     * @param request    the sessions to add
     * @return the updated offering response
     */
    @Transactional
    public OfferingResponse addSessions(Long offeringId, AddSessionsRequest request) {
        log.info("Adding {} sessions to offering {}", request.getSessions().size(), offeringId);

        Offering offering = offeringRepository.findByIdWithSessions(offeringId)
                .orElseThrow(() -> new ResourceNotFoundException("Offering", offeringId));

        ZoneId teacherZone = ZoneId.of(offering.getTeacherTimezone());
        List<Session> newSessions = new ArrayList<>();

        for (AddSessionsRequest.SessionEntry entry : request.getSessions()) {
            // Parse times as local date-time in the teacher's timezone
            LocalDateTime localStart = parseLocalDateTime(entry.getStartTime());
            LocalDateTime localEnd = parseLocalDateTime(entry.getEndTime());

            // Convert teacher-local times to UTC Instant
            Instant startUtc = localStart.atZone(teacherZone).toInstant();
            Instant endUtc = localEnd.atZone(teacherZone).toInstant();

            // Validate: end must be after start
            if (!endUtc.isAfter(startUtc)) {
                throw new IllegalArgumentException(
                        "Session end time must be after start time: " + entry.getStartTime() +
                        " → " + entry.getEndTime());
            }

            // Validate: session should not be in the past
            if (startUtc.isBefore(Instant.now())) {
                throw new IllegalArgumentException(
                        "Session start time cannot be in the past: " + entry.getStartTime());
            }

            Session session = Session.builder()
                    .offering(offering)
                    .startTime(startUtc)
                    .endTime(endUtc)
                    .build();

            newSessions.add(session);
        }

        sessionRepository.saveAll(newSessions);
        offering.getSessions().addAll(newSessions);

        log.info("Added {} sessions to offering {}", newSessions.size(), offeringId);
        return toOfferingResponse(offering, teacherZone);
    }

    /**
     * Gets all offerings for a specific teacher.
     *
     * @param teacherId the teacher's ID
     * @return list of offerings with sessions
     */
    @Transactional(readOnly = true)
    public List<OfferingResponse> getTeacherOfferings(Long teacherId) {
        log.debug("Fetching offerings for teacher {}", teacherId);

        List<Offering> offerings = offeringRepository.findByTeacherIdWithSessions(teacherId);

        return offerings.stream()
                .map(o -> toOfferingResponse(o, ZoneId.of(o.getTeacherTimezone())))
                .collect(Collectors.toList());
    }

    /**
     * Gets a single offering by ID with full session details.
     *
     * @param offeringId the offering ID
     * @param timezone   optional timezone for display (defaults to teacher's timezone)
     * @return offering details
     */
    @Transactional(readOnly = true)
    public OfferingResponse getOfferingById(Long offeringId, String timezone) {
        Offering offering = offeringRepository.findByIdWithSessions(offeringId)
                .orElseThrow(() -> new ResourceNotFoundException("Offering", offeringId));

        ZoneId displayZone = (timezone != null && !timezone.isBlank())
                ? validateTimezone(timezone)
                : ZoneId.of(offering.getTeacherTimezone());

        return toOfferingResponse(offering, displayZone);
    }

    // ======================== Helper Methods ========================

    /**
     * Converts an Offering entity to an OfferingResponse DTO.
     * Session times are converted to the specified display timezone.
     */
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

    /**
     * Converts a Session entity to a SessionResponse DTO with timezone conversion.
     */
    private SessionResponse toSessionResponse(Session session, ZoneId displayZone) {
        ZonedDateTime start = session.getStartTime().atZone(displayZone);
        ZonedDateTime end = session.getEndTime().atZone(displayZone);
        long durationMinutes = Duration.between(start, end).toMinutes();

        return SessionResponse.builder()
                .id(session.getId())
                .offeringId(session.getOffering().getId())
                .startTime(start)
                .endTime(end)
                .durationMinutes(durationMinutes)
                .build();
    }

    /**
     * Validates and returns a ZoneId from a timezone string.
     */
    private ZoneId validateTimezone(String timezone) {
        try {
            return ZoneId.of(timezone);
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("Invalid timezone: '" + timezone +
                    "'. Use IANA timezone IDs like 'America/New_York' or 'Asia/Kolkata'.");
        }
    }

    /**
     * Parses a local date-time string (ISO-8601 without timezone).
     */
    private LocalDateTime parseLocalDateTime(String dateTimeStr) {
        try {
            return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Invalid date-time format: '" + dateTimeStr +
                    "'. Expected ISO-8601 format like '2026-06-07T18:00:00'.");
        }
    }
}

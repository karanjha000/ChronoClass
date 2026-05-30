/*
 * Copyright (c) 2026 Karan Jha. All rights reserved.
 * This software is submitted for evaluation purposes only.
 * Unauthorized commercial use, reproduction, or distribution is prohibited.
 */
package com.chronoclass.service;

import com.chronoclass.dto.request.BookOfferingRequest;
import com.chronoclass.dto.request.CreateOfferingRequest;
import com.chronoclass.dto.request.AddSessionsRequest;
import com.chronoclass.dto.response.BookingResponse;
import com.chronoclass.dto.response.OfferingResponse;
import com.chronoclass.exception.ConcurrentBookingException;
import com.chronoclass.exception.TimeConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for concurrent booking scenarios.
 * Tests verify that the pessimistic locking strategy correctly prevents:
 * 1. Overbooking (multiple parents booking when capacity = 1)
 * 2. Double booking (same parent booking overlapping offerings)
 * 3. Data consistency under concurrent load
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Booking Concurrency Tests")
class BookingConcurrencyTest {

    @Autowired
    private TeacherService teacherService;

    @Autowired
    private ParentService parentService;

    private static final String TEACHER_TZ = "America/New_York";
    private static final String PARENT_TZ = "Asia/Kolkata";
    private static final Long TEACHER_ID = 1L;

    /**
     * Helper to create a future date-time string.
     */
    private String futureDateTime(int daysFromNow, int hour) {
        LocalDateTime dt = LocalDateTime.now().plusDays(daysFromNow).withHour(hour).withMinute(0).withSecond(0);
        return dt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    /**
     * Creates an offering with sessions for testing.
     */
    private OfferingResponse createTestOffering(String courseName, String title, int maxStudents,
                                                 int startDay, int startHour, int sessions) {
        CreateOfferingRequest req = CreateOfferingRequest.builder()
                .courseName(courseName)
                .courseDescription("Test course")
                .title(title)
                .teacherTimezone(TEACHER_TZ)
                .maxStudents(maxStudents)
                .build();

        OfferingResponse offering = teacherService.createOffering(TEACHER_ID, req);

        List<AddSessionsRequest.SessionEntry> sessionEntries = new ArrayList<>();
        for (int i = 0; i < sessions; i++) {
            sessionEntries.add(AddSessionsRequest.SessionEntry.builder()
                    .startTime(futureDateTime(startDay + (i * 7), startHour))
                    .endTime(futureDateTime(startDay + (i * 7), startHour + 1))
                    .build());
        }

        AddSessionsRequest addReq = AddSessionsRequest.builder().sessions(sessionEntries).build();
        return teacherService.addSessions(offering.getId(), addReq);
    }

    @Test
    @DisplayName("Multiple parents can book the same offering concurrently (within capacity)")
    void testMultipleParentsBookSameOffering() throws Exception {
        // Create offering with capacity for 5 students
        OfferingResponse offering = createTestOffering(
                "Concurrent Test Course 1", "Multi-Parent Batch", 5, 10, 18, 3);

        int numParents = 5;
        ExecutorService executor = Executors.newFixedThreadPool(numParents);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < numParents; i++) {
            final long parentId = 1000L + i;
            futures.add(executor.submit(() -> {
                try {
                    latch.await(); // Wait for all threads to be ready
                    BookOfferingRequest bookReq = BookOfferingRequest.builder()
                            .offeringId(offering.getId())
                            .parentTimezone(PARENT_TZ)
                            .build();
                    parentService.bookOffering(parentId, bookReq);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                }
            }));
        }

        latch.countDown(); // Start all threads simultaneously
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        // All 5 should succeed since capacity is 5
        assertEquals(numParents, successCount.get(),
                "All parents should successfully book within capacity");
        assertEquals(0, failCount.get(), "No failures expected");
    }

    @Test
    @DisplayName("Overbooking is prevented when capacity is exceeded")
    void testOverbookingPrevention() throws Exception {
        // Create offering with capacity for only 2 students
        OfferingResponse offering = createTestOffering(
                "Concurrent Test Course 2", "Limited Batch", 2, 20, 14, 2);

        int numParents = 5;
        ExecutorService executor = Executors.newFixedThreadPool(numParents);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < numParents; i++) {
            final long parentId = 2000L + i;
            executor.submit(() -> {
                try {
                    latch.await();
                    BookOfferingRequest bookReq = BookOfferingRequest.builder()
                            .offeringId(offering.getId())
                            .parentTimezone(PARENT_TZ)
                            .build();
                    parentService.bookOffering(parentId, bookReq);
                    successCount.incrementAndGet();
                } catch (ConcurrentBookingException e) {
                    failCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                }
            });
        }

        latch.countDown();
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        // Only 2 should succeed
        assertEquals(2, successCount.get(),
                "Only 2 bookings should succeed (capacity = 2)");
        assertEquals(3, failCount.get(),
                "3 bookings should fail due to capacity");
    }

    @Test
    @DisplayName("Time conflict prevents overlapping bookings for same parent")
    void testTimeConflictDetection() {
        // Create two offerings with overlapping sessions
        OfferingResponse offering1 = createTestOffering(
                "Conflict Test Course A", "Saturday Batch", 30, 30, 17, 3);

        OfferingResponse offering2 = createTestOffering(
                "Conflict Test Course B", "Saturday Overlap Batch", 30, 30, 17, 3);

        Long parentId = 3000L;

        // Book first offering — should succeed
        BookOfferingRequest bookReq1 = BookOfferingRequest.builder()
                .offeringId(offering1.getId())
                .parentTimezone(PARENT_TZ)
                .build();
        BookingResponse booking1 = parentService.bookOffering(parentId, bookReq1);
        assertNotNull(booking1);
        assertEquals("CONFIRMED", booking1.getStatus());

        // Book second offering with overlapping times — should fail
        BookOfferingRequest bookReq2 = BookOfferingRequest.builder()
                .offeringId(offering2.getId())
                .parentTimezone(PARENT_TZ)
                .build();

        TimeConflictException exception = assertThrows(TimeConflictException.class,
                () -> parentService.bookOffering(parentId, bookReq2));

        assertTrue(exception.getMessage().contains("overlap"));
        assertFalse(exception.getConflicts().isEmpty());
    }

    @Test
    @DisplayName("Same parent cannot book the same offering twice")
    void testDuplicateBookingPrevention() {
        OfferingResponse offering = createTestOffering(
                "Duplicate Test Course", "Evening Batch", 30, 40, 19, 2);

        Long parentId = 4000L;

        // First booking — should succeed
        BookOfferingRequest bookReq = BookOfferingRequest.builder()
                .offeringId(offering.getId())
                .parentTimezone(PARENT_TZ)
                .build();
        parentService.bookOffering(parentId, bookReq);

        // Second booking of same offering — should fail
        assertThrows(ConcurrentBookingException.class,
                () -> parentService.bookOffering(parentId, bookReq));
    }

    @Test
    @DisplayName("Non-overlapping offerings can be booked by the same parent")
    void testNonOverlappingBookings() {
        // Morning offering
        OfferingResponse morning = createTestOffering(
                "Morning Course", "Morning Batch", 30, 50, 9, 2);

        // Evening offering (no overlap with morning)
        OfferingResponse evening = createTestOffering(
                "Evening Course", "Evening Batch", 30, 50, 20, 2);

        Long parentId = 5000L;

        // Book morning — should succeed
        BookOfferingRequest morningReq = BookOfferingRequest.builder()
                .offeringId(morning.getId())
                .parentTimezone(PARENT_TZ)
                .build();
        BookingResponse booking1 = parentService.bookOffering(parentId, morningReq);
        assertNotNull(booking1);

        // Book evening — should also succeed (no overlap)
        BookOfferingRequest eveningReq = BookOfferingRequest.builder()
                .offeringId(evening.getId())
                .parentTimezone(PARENT_TZ)
                .build();
        BookingResponse booking2 = parentService.bookOffering(parentId, eveningReq);
        assertNotNull(booking2);
    }
}

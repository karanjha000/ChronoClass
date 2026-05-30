/*
 * Copyright (c) 2026 ChronoClass. All rights reserved.
 * This software is submitted for evaluation purposes only.
 * Unauthorized commercial use, reproduction, or distribution is prohibited.
 */
package com.chronoclass.controller;

import com.chronoclass.dto.request.BookOfferingRequest;
import com.chronoclass.dto.response.BookingResponse;
import com.chronoclass.dto.response.OfferingResponse;
import com.chronoclass.service.ParentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for parent/student operations.
 * Parents can view available offerings, book offerings, and view their bookings.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Parent APIs", description = "Endpoints for parents/students to browse and book offerings")
public class ParentController {

    private final ParentService parentService;

    /**
     * Get all available offerings.
     * Session times are displayed in the parent's local timezone.
     */
    @GetMapping("/offerings")
    @Operation(summary = "Get available offerings",
               description = "Returns all active offerings with available capacity. " +
                       "Session times are converted to the specified timezone. " +
                       "Default timezone is UTC if not specified.")
    public ResponseEntity<List<OfferingResponse>> getAvailableOfferings(
            @Parameter(description = "Parent's timezone (IANA ID) for session time display",
                       example = "Asia/Kolkata")
            @RequestParam(defaultValue = "UTC") String timezone) {

        List<OfferingResponse> offerings = parentService.getAvailableOfferings(timezone);
        return ResponseEntity.ok(offerings);
    }

    /**
     * Book an offering for a parent.
     * This is a concurrency-safe operation that handles:
     * - Capacity checks
     * - Duplicate booking prevention
     * - Time conflict detection across all booked sessions
     */
    @PostMapping("/parents/{parentId}/bookings")
    @Operation(summary = "Book an offering",
               description = "Books an entire offering for a parent. " +
                       "All session times of the offering are locked for this parent. " +
                       "The system checks for time conflicts with existing bookings " +
                       "and handles concurrent booking attempts safely.")
    public ResponseEntity<BookingResponse> bookOffering(
            @Parameter(description = "Parent ID", example = "101")
            @PathVariable Long parentId,
            @Valid @RequestBody BookOfferingRequest request) {

        BookingResponse response = parentService.bookOffering(parentId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all bookings for a parent.
     * Session times are shown in the parent's timezone.
     */
    @GetMapping("/parents/{parentId}/bookings")
    @Operation(summary = "Get parent's bookings",
               description = "Returns all confirmed bookings for a parent with " +
                       "offering details and session times in the specified timezone.")
    public ResponseEntity<List<BookingResponse>> getParentBookings(
            @Parameter(description = "Parent ID", example = "101")
            @PathVariable Long parentId,
            @Parameter(description = "Override timezone for display (IANA ID)",
                       example = "Asia/Kolkata")
            @RequestParam(required = false) String timezone) {

        List<BookingResponse> bookings = parentService.getParentBookings(parentId, timezone);
        return ResponseEntity.ok(bookings);
    }
}

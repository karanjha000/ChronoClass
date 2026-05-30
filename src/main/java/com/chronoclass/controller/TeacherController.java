/*
 * Copyright (c) 2026 Karan Jha. All rights reserved.
 * This software is submitted for evaluation purposes only.
 * Unauthorized commercial use, reproduction, or distribution is prohibited.
 */
package com.chronoclass.controller;

import com.chronoclass.dto.request.AddSessionsRequest;
import com.chronoclass.dto.request.CreateOfferingRequest;
import com.chronoclass.dto.response.OfferingResponse;
import com.chronoclass.service.TeacherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Teacher APIs", description = "Endpoints for teachers to manage offerings and sessions")
public class TeacherController {

    private final TeacherService teacherService;

    public TeacherController(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    @PostMapping("/teachers/{teacherId}/offerings")
    @Operation(summary = "Create a new offering",
               description = "Creates a new offering/section for a course. " +
                       "The course is created automatically if it doesn't exist.")
    public ResponseEntity<OfferingResponse> createOffering(
            @Parameter(description = "Teacher ID", example = "1")
            @PathVariable Long teacherId,
            @Valid @RequestBody CreateOfferingRequest request) {

        OfferingResponse response = teacherService.createOffering(teacherId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/offerings/{offeringId}/sessions")
    @Operation(summary = "Add sessions to an offering",
               description = "Adds one or more sessions to an existing offering. " +
                       "Times should be provided in the teacher's local timezone " +
                       "(as specified during offering creation). " +
                       "They will be stored in UTC internally.")
    public ResponseEntity<OfferingResponse> addSessions(
            @Parameter(description = "Offering ID", example = "1")
            @PathVariable Long offeringId,
            @Valid @RequestBody AddSessionsRequest request) {

        OfferingResponse response = teacherService.addSessions(offeringId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/teachers/{teacherId}/offerings")
    @Operation(summary = "Get teacher's offerings",
               description = "Returns all offerings created by the specified teacher, " +
                       "with session times displayed in the teacher's timezone.")
    public ResponseEntity<List<OfferingResponse>> getTeacherOfferings(
            @Parameter(description = "Teacher ID", example = "1")
            @PathVariable Long teacherId) {

        List<OfferingResponse> offerings = teacherService.getTeacherOfferings(teacherId);
        return ResponseEntity.ok(offerings);
    }

    @GetMapping("/offerings/{offeringId}")
    @Operation(summary = "Get offering details",
               description = "Returns detailed information about a specific offering, " +
                       "including all sessions. Optionally specify a timezone for display.")
    public ResponseEntity<OfferingResponse> getOffering(
            @Parameter(description = "Offering ID", example = "1")
            @PathVariable Long offeringId,
            @Parameter(description = "Timezone for display (IANA ID)", example = "Asia/Kolkata")
            @RequestParam(required = false) String timezone) {

        OfferingResponse response = teacherService.getOfferingById(offeringId, timezone);
        return ResponseEntity.ok(response);
    }
}

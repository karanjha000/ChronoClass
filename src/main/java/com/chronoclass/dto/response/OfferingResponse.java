/*
 * Copyright (c) 2026 ChronoClass. All rights reserved.
 * This software is submitted for evaluation purposes only.
 * Unauthorized commercial use, reproduction, or distribution is prohibited.
 */
package com.chronoclass.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Response DTO for an offering with its sessions.
 * Session times are converted to the requested timezone.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Offering details with session schedule")
public class OfferingResponse {

    @Schema(description = "Offering ID")
    private Long id;

    @Schema(description = "Course name")
    private String courseName;

    @Schema(description = "Course description")
    private String courseDescription;

    @Schema(description = "Offering title (e.g., Saturday Batch)")
    private String title;

    @Schema(description = "Teacher ID")
    private Long teacherId;

    @Schema(description = "Teacher's timezone")
    private String teacherTimezone;

    @Schema(description = "Maximum number of students")
    private Integer maxStudents;

    @Schema(description = "Current enrollment count")
    private Integer enrolledCount;

    @Schema(description = "Available spots remaining")
    private Integer availableSpots;

    @Schema(description = "Offering status")
    private String status;

    @Schema(description = "Timezone used for displaying session times")
    private String displayTimezone;

    @Schema(description = "List of sessions")
    private List<SessionResponse> sessions;

    @Schema(description = "Offering creation time")
    private ZonedDateTime createdAt;
}

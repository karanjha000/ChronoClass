/*
 * Copyright (c) 2026 Karan Jha. All rights reserved.
 * This software is submitted for evaluation purposes only.
 * Unauthorized commercial use, reproduction, or distribution is prohibited.
 */
package com.chronoclass.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to create a new course offering")
public class CreateOfferingRequest {

    @NotBlank(message = "Course name is required")
    @Schema(description = "Name of the course", example = "Python Coding")
    private String courseName;

    @Schema(description = "Description of the course", example = "Learn Python from scratch")
    private String courseDescription;

    @NotBlank(message = "Offering title is required")
    @Schema(description = "Title of this offering/batch", example = "Saturday Batch")
    private String title;

    @NotBlank(message = "Teacher timezone is required")
    @Schema(description = "IANA timezone of the teacher", example = "America/New_York")
    private String teacherTimezone;

    @Min(value = 1, message = "Max students must be at least 1")
    @Schema(description = "Maximum number of students", example = "30")
    @Builder.Default
    private Integer maxStudents = 30;
}

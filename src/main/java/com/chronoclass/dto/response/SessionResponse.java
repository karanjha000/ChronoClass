/*
 * Copyright (c) 2026 Karan Jha. All rights reserved.
 * This software is submitted for evaluation purposes only.
 * Unauthorized commercial use, reproduction, or distribution is prohibited.
 */
package com.chronoclass.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Session details with timezone-converted times")
public class SessionResponse {

    @Schema(description = "Session ID")
    private Long id;

    @Schema(description = "Offering ID this session belongs to")
    private Long offeringId;

    @Schema(description = "Session start time in the display timezone")
    private ZonedDateTime startTime;

    @Schema(description = "Session end time in the display timezone")
    private ZonedDateTime endTime;

    @Schema(description = "Duration in minutes")
    private Long durationMinutes;
}

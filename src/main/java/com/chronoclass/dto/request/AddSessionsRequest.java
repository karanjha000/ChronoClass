/*
 * Copyright (c) 2026 Karan Jha. All rights reserved.
 * This software is submitted for evaluation purposes only.
 * Unauthorized commercial use, reproduction, or distribution is prohibited.
 */
package com.chronoclass.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to add sessions to an offering")
public class AddSessionsRequest {

    @NotEmpty(message = "At least one session is required")
    @Valid
    @Schema(description = "List of sessions to add")
    private List<SessionEntry> sessions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "A single session entry with start and end times in the teacher's timezone")
    public static class SessionEntry {

        @Schema(description = "Session start time in teacher's timezone (ISO-8601)",
                example = "2026-06-07T18:00:00")
        @jakarta.validation.constraints.NotNull(message = "Start time is required")
        private String startTime;

        @Schema(description = "Session end time in teacher's timezone (ISO-8601)",
                example = "2026-06-07T19:00:00")
        @jakarta.validation.constraints.NotNull(message = "End time is required")
        private String endTime;
    }
}

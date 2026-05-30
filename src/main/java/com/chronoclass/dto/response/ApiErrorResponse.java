/*
 * Copyright (c) 2026 ChronoClass. All rights reserved.
 * This software is submitted for evaluation purposes only.
 * Unauthorized commercial use, reproduction, or distribution is prohibited.
 */
package com.chronoclass.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;
import java.util.List;

/**
 * Standardized API error response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standardized error response")
public class ApiErrorResponse {

    @Schema(description = "HTTP status code")
    private int status;

    @Schema(description = "Error type")
    private String error;

    @Schema(description = "Error message")
    private String message;

    @Schema(description = "Detailed error messages (for validation errors)")
    private List<String> details;

    @Schema(description = "Request path that caused the error")
    private String path;

    @Schema(description = "Timestamp of the error")
    @Builder.Default
    private Instant timestamp = Instant.now();
}

/*
 * Copyright (c) 2026 Karan Jha. All rights reserved.
 * This software is submitted for evaluation purposes only.
 * Unauthorized commercial use, reproduction, or distribution is prohibited.
 */
package com.chronoclass.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to book an offering")
public class BookOfferingRequest {

    @NotNull(message = "Offering ID is required")
    @Schema(description = "ID of the offering to book", example = "1")
    private Long offeringId;

    @NotBlank(message = "Parent timezone is required")
    @Schema(description = "IANA timezone of the parent", example = "Asia/Kolkata")
    private String parentTimezone;
}

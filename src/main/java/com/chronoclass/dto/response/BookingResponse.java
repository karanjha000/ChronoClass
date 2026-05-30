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
 * Response DTO for a booking confirmation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Booking confirmation details")
public class BookingResponse {

    @Schema(description = "Booking ID")
    private Long id;

    @Schema(description = "Parent ID")
    private Long parentId;

    @Schema(description = "Parent's timezone")
    private String parentTimezone;

    @Schema(description = "Booking status")
    private String status;

    @Schema(description = "Offering details")
    private OfferingResponse offering;

    @Schema(description = "Booking creation time")
    private ZonedDateTime bookedAt;
}

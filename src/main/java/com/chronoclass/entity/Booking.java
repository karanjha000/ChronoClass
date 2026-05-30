/*
 * Copyright (c) 2026 ChronoClass. All rights reserved.
 * This software is submitted for evaluation purposes only.
 * Unauthorized commercial use, reproduction, or distribution is prohibited.
 */
package com.chronoclass.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Represents a parent's booking of an offering.
 * Booking is at the offering level — booking an offering means booking ALL its sessions.
 */
@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offering_id", nullable = false)
    private Offering offering;

    @Column(name = "parent_id", nullable = false)
    private Long parentId;

    @Column(name = "parent_timezone", nullable = false)
    private String parentTimezone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BookingStatus status = BookingStatus.CONFIRMED;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    public enum BookingStatus {
        CONFIRMED, CANCELLED
    }
}

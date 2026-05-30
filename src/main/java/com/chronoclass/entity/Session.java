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
 * Represents an actual meeting time belonging to an offering.
 * Times are stored in UTC and converted to local timezones on read.
 */
@Entity
@Table(name = "sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offering_id", nullable = false)
    private Offering offering;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    /**
     * Checks whether this session overlaps with another session.
     * Two sessions overlap if one starts before the other ends AND ends after the other starts.
     */
    public boolean overlapsWith(Session other) {
        return this.startTime.isBefore(other.endTime) && this.endTime.isAfter(other.startTime);
    }
}

/*
 * Copyright (c) 2026 ChronoClass. All rights reserved.
 * This software is submitted for evaluation purposes only.
 * Unauthorized commercial use, reproduction, or distribution is prohibited.
 */
package com.chronoclass.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a schedulable offering/section of a course.
 * For example: "Saturday Batch" or "Summer Camp Weekday Batch".
 * Contains multiple sessions (actual meeting times).
 */
@Entity
@Table(name = "offerings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Offering {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    @Column(nullable = false)
    private String title;

    @Column(name = "teacher_timezone", nullable = false)
    private String teacherTimezone;

    @Column(name = "max_students", nullable = false)
    @Builder.Default
    private Integer maxStudents = 30;

    @Column(name = "enrolled_count", nullable = false)
    @Builder.Default
    private Integer enrolledCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OfferingStatus status = OfferingStatus.ACTIVE;

    @Version
    private Long version;

    @OneToMany(mappedBy = "offering", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Session> sessions = new ArrayList<>();

    @OneToMany(mappedBy = "offering", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Booking> bookings = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public boolean hasCapacity() {
        return this.enrolledCount < this.maxStudents;
    }

    public void incrementEnrolledCount() {
        this.enrolledCount++;
    }

    public enum OfferingStatus {
        ACTIVE, CANCELLED, COMPLETED
    }
}

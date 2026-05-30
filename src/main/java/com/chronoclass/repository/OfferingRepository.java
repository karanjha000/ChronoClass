/*
 * Copyright (c) 2026 ChronoClass. All rights reserved.
 * This software is submitted for evaluation purposes only.
 * Unauthorized commercial use, reproduction, or distribution is prohibited.
 */
package com.chronoclass.repository;

import com.chronoclass.entity.Offering;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OfferingRepository extends JpaRepository<Offering, Long> {

    /**
     * Find all offerings by a specific teacher, ordered by creation date descending.
     */
    @Query("SELECT DISTINCT o FROM Offering o LEFT JOIN FETCH o.sessions LEFT JOIN FETCH o.course " +
           "WHERE o.teacherId = :teacherId ORDER BY o.createdAt DESC")
    List<Offering> findByTeacherIdWithSessions(@Param("teacherId") Long teacherId);

    /**
     * Find all active offerings with their sessions and course info.
     */
    @Query("SELECT DISTINCT o FROM Offering o LEFT JOIN FETCH o.sessions LEFT JOIN FETCH o.course " +
           "WHERE o.status = 'ACTIVE' ORDER BY o.createdAt DESC")
    List<Offering> findAllActiveWithSessions();

    /**
     * Find an offering by ID with pessimistic write lock for concurrent booking.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Offering o WHERE o.id = :id")
    Optional<Offering> findByIdForUpdate(@Param("id") Long id);

    /**
     * Find offering by ID with sessions eagerly loaded.
     */
    @Query("SELECT o FROM Offering o LEFT JOIN FETCH o.sessions LEFT JOIN FETCH o.course WHERE o.id = :id")
    Optional<Offering> findByIdWithSessions(@Param("id") Long id);
}

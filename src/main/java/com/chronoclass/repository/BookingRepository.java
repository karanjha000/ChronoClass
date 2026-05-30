/*
 * Copyright (c) 2026 Karan Jha. All rights reserved.
 * This software is submitted for evaluation purposes only.
 * Unauthorized commercial use, reproduction, or distribution is prohibited.
 */
package com.chronoclass.repository;

import com.chronoclass.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("SELECT b FROM Booking b JOIN FETCH b.offering o LEFT JOIN FETCH o.sessions LEFT JOIN FETCH o.course " +
           "WHERE b.parentId = :parentId AND b.status = 'CONFIRMED' ORDER BY b.createdAt DESC")
    List<Booking> findByParentIdWithOfferings(@Param("parentId") Long parentId);

    @Query("SELECT COUNT(b) > 0 FROM Booking b WHERE b.parentId = :parentId " +
           "AND b.offering.id = :offeringId AND b.status = 'CONFIRMED'")
    boolean existsByParentIdAndOfferingId(@Param("parentId") Long parentId,
                                          @Param("offeringId") Long offeringId);
}

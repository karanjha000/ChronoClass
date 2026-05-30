/*
 * Copyright (c) 2026 Karan Jha. All rights reserved.
 * This software is submitted for evaluation purposes only.
 * Unauthorized commercial use, reproduction, or distribution is prohibited.
 */
package com.chronoclass.repository;

import com.chronoclass.entity.Session;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {

    List<Session> findByOfferingId(Long offeringId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Session s WHERE s.offering.id = :offeringId")
    List<Session> findByOfferingIdForUpdate(@Param("offeringId") Long offeringId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Session s WHERE s.offering.id IN " +
           "(SELECT b.offering.id FROM Booking b WHERE b.parentId = :parentId AND b.status = 'CONFIRMED')")
    List<Session> findBookedSessionsByParentIdForUpdate(@Param("parentId") Long parentId);

    @Query("SELECT s FROM Session s WHERE s.offering.id IN " +
           "(SELECT b.offering.id FROM Booking b WHERE b.parentId = :parentId AND b.status = 'CONFIRMED')")
    List<Session> findBookedSessionsByParentId(@Param("parentId") Long parentId);
}

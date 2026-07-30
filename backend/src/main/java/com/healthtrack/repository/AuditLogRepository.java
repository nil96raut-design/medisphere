package com.healthtrack.repository;

import com.healthtrack.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByHospitalIdOrderByTimestampDesc(Long hospitalId);

    List<AuditLog> findByUserId(Long userId);

    @Query("""
        select a from AuditLog a where a.hospitalId = :hospitalId
        and (:action is null or a.action = :action)
        and (:userId is null or a.userId = :userId)
        and (:from is null or a.timestamp >= :from)
        and (:to is null or a.timestamp <= :to)
        order by a.timestamp desc
    """)
    Page<AuditLog> search(
            @Param("hospitalId") Long hospitalId,
            @Param("action") String action,
            @Param("userId") Long userId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            Pageable pageable);
}
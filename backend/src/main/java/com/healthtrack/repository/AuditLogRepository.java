package com.healthtrack.repository;

import com.healthtrack.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByHospitalIdOrderByTimestampDesc(Long hospitalId);
}

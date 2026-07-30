package com.healthtrack.repository;

import com.healthtrack.entity.Alert;
import com.healthtrack.entity.AlertStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findByPatientIdOrderByCreatedAtDesc(Long patientId);

    List<Alert> findByStatusOrderByCreatedAtAsc(AlertStatus status);

    List<Alert> findByStatusAndCreatedAtBefore(AlertStatus status, OffsetDateTime threshold);

    long countByPatientIdAndStatusIn(Long patientId, List<AlertStatus> statuses);

    @Query("SELECT a FROM Alert a JOIN FETCH a.patient JOIN FETCH a.hospital WHERE a.status = :status ORDER BY a.createdAt ASC")
    List<Alert> findByStatusFetching(@Param("status") AlertStatus status);
}

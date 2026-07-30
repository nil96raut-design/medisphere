package com.healthtrack.repository;

import com.healthtrack.entity.VitalRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VitalRecordRepository extends JpaRepository<VitalRecord, Long> {

    List<VitalRecord> findByPatientIdOrderByRecordedAtDesc(Long patientId);

    List<VitalRecord> findByPatientIdAndAlertFlagTrue(Long patientId);

    long countByPatientId(Long patientId);
}

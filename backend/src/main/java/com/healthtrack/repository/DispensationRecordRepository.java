package com.healthtrack.repository;

import com.healthtrack.entity.BillingStatus;
import com.healthtrack.entity.DispensationRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DispensationRecordRepository extends JpaRepository<DispensationRecord, Long> {

    List<DispensationRecord> findByPatientIdAndBillingStatus(Long patientId, BillingStatus billingStatus);

    boolean existsByPatientIdAndBillingStatus(Long patientId, BillingStatus billingStatus);
}

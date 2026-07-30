package com.healthtrack.repository;

import com.healthtrack.entity.BillingStatus;
import com.healthtrack.entity.DispensationRecord;
import com.healthtrack.entity.DispensationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface DispensationRecordRepository extends JpaRepository<DispensationRecord, Long> {

    List<DispensationRecord> findByPatientIdAndBillingStatus(Long patientId, BillingStatus billingStatus);

    boolean existsByPatientIdAndBillingStatus(Long patientId, BillingStatus billingStatus);

    long countByHospitalId(Long hospitalId);

    @Query("select d from DispensationRecord d where d.hospital.id = :hospitalId and d.dispensedAt >= :since")
    List<DispensationRecord> findByHospitalIdAndDispensedAtAfter(
            @Param("hospitalId") Long hospitalId, @Param("since") LocalDateTime since);

    List<DispensationRecord> findByPrescriptionItemId(Long prescriptionItemId);

    @Query("select coalesce(sum(d.quantityDispensed), 0) from DispensationRecord d " +
           "where d.prescriptionItem.id = :prescriptionItemId")
    int sumQuantityDispensedByPrescriptionItemId(@Param("prescriptionItemId") Long prescriptionItemId);

    List<DispensationRecord> findByPatientIdOrderByDispensedAtDesc(Long patientId);

    Page<DispensationRecord> findByHospitalId(Long hospitalId, Pageable pageable);

    List<DispensationRecord> findByPrescriptionItemIdAndDispensationStatus(
            Long prescriptionItemId, DispensationStatus status);
}
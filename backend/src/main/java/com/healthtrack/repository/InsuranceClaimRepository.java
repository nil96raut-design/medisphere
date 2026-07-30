package com.healthtrack.repository;

import com.healthtrack.entity.InsuranceClaim;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InsuranceClaimRepository extends JpaRepository<InsuranceClaim, Long> {

    List<InsuranceClaim> findByBillId(Long billId);

    Page<InsuranceClaim> findByHospitalId(Long hospitalId, Pageable pageable);

    Page<InsuranceClaim> findByHospitalIdAndStatus(Long hospitalId, String status, Pageable pageable);

    long countByHospitalIdAndStatus(Long hospitalId, String status);
}

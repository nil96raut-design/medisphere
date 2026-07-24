package com.healthtrack.repository;

import com.healthtrack.entity.BillingTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BillingTransactionRepository extends JpaRepository<BillingTransaction, Long> {

    List<BillingTransaction> findByPatientId(Long patientId);
}

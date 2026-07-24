package com.healthtrack.repository;

import com.healthtrack.entity.LabOrderStatus;
import com.healthtrack.entity.LabTestOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LabTestOrderRepository extends JpaRepository<LabTestOrder, Long> {

    List<LabTestOrder> findByStatusOrderByCreatedAtDesc(LabOrderStatus status);

    long countByStatus(LabOrderStatus status);

    List<LabTestOrder> findByPatientIdOrderByCreatedAtDesc(Long patientId);
}

package com.healthtrack.repository;

import com.healthtrack.entity.PurchaseOrder;
import com.healthtrack.entity.PurchaseOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    List<PurchaseOrder> findByHospitalIdOrderByOrderedAtDesc(Long hospitalId);

    List<PurchaseOrder> findByHospitalIdAndStatus(Long hospitalId, PurchaseOrderStatus status);

    long countByHospitalIdAndStatus(Long hospitalId, PurchaseOrderStatus status);
}

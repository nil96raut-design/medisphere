package com.healthtrack.repository;

import com.healthtrack.entity.BilledItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BilledItemRepository extends JpaRepository<BilledItem, Long> {

    Optional<BilledItem> findBySourceTypeAndSourceId(String sourceType, Long sourceId);

    List<BilledItem> findByPatientId(Long patientId);

    List<BilledItem> findByBillId(Long billId);

    boolean existsBySourceTypeAndSourceId(String sourceType, Long sourceId);
}

package com.healthtrack.repository;

import com.healthtrack.entity.LabResultHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LabResultHistoryRepository extends JpaRepository<LabResultHistory, Long> {

    List<LabResultHistory> findByLabOrderIdOrderByVersionDesc(Long labOrderId);

    Optional<LabResultHistory> findByLabOrderIdAndIsActiveTrue(Long labOrderId);

    int countByLabOrderId(Long labOrderId);
}

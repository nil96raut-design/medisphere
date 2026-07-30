package com.healthtrack.repository;

import com.healthtrack.entity.PharmacyRecall;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface PharmacyRecallRepository extends JpaRepository<PharmacyRecall, Long> {
    Optional<PharmacyRecall> findByMedicineNameAndBatchNumberAndActiveTrue(String medicineName, String batchNumber);
    List<PharmacyRecall> findByActiveTrue();
}

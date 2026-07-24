package com.healthtrack.repository;

import com.healthtrack.entity.PrescriptionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PrescriptionItemRepository extends JpaRepository<PrescriptionItem, Long> {

    @Query("SELECT p FROM PrescriptionItem p WHERE p.hospital.id = :hospitalId")
    List<PrescriptionItem> findPendingByHospital(@Param("hospitalId") Long hospitalId);
}

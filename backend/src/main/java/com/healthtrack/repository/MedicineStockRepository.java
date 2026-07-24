package com.healthtrack.repository;

import com.healthtrack.entity.MedicineStock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MedicineStockRepository extends JpaRepository<MedicineStock, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from MedicineStock m where m.id = :id")
    Optional<MedicineStock> findByIdLocked(@Param("id") Long id);

    List<MedicineStock> findByAvailableQuantityLessThanEqual(Integer reorderLevel);

    long countByAvailableQuantityLessThanEqual(Integer reorderLevel);
}

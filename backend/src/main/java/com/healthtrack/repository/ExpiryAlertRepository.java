package com.healthtrack.repository;

import com.healthtrack.entity.ExpiryAlert;
import com.healthtrack.entity.ExpiryAlertType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ExpiryAlertRepository extends JpaRepository<ExpiryAlert, Long> {

    List<ExpiryAlert> findByHospitalIdAndIsResolvedFalse(Long hospitalId);

    List<ExpiryAlert> findByHospitalIdAndAlertTypeAndIsResolvedFalse(Long hospitalId, ExpiryAlertType type);

    boolean existsByMedicineStockIdAndAlertTypeAndIsResolvedFalse(Long medicineStockId, ExpiryAlertType type);

    List<ExpiryAlert> findByHospitalIdAndExpiryDateBeforeAndIsResolvedFalse(Long hospitalId, LocalDate date);

    List<ExpiryAlert> findByHospitalIdOrderByExpiryDateAsc(Long hospitalId);
}

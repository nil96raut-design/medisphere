package com.healthtrack.repository;

import com.healthtrack.entity.MedicineStock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MedicineStockRepository extends JpaRepository<MedicineStock, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from MedicineStock m where m.id = :id")
    Optional<MedicineStock> findByIdLocked(@Param("id") Long id);

    List<MedicineStock> findByAvailableQuantityLessThanEqual(Integer reorderLevel);

    long countByAvailableQuantityLessThanEqual(Integer reorderLevel);

    List<MedicineStock> findByHospitalId(Long hospitalId);

    @Query("select m from MedicineStock m where m.hospital.id = :hospitalId and m.availableQuantity <= m.reorderLevel")
    List<MedicineStock> findLowStockByHospitalId(@Param("hospitalId") Long hospitalId);

    @Query("select count(m) from MedicineStock m where m.hospital.id = :hospitalId and m.availableQuantity <= m.reorderLevel")
    long countLowStockByHospitalId(@Param("hospitalId") Long hospitalId);

    List<MedicineStock> findByHospitalIdAndExpiryDateBefore(Long hospitalId, LocalDate expiryDate);

    long countByHospitalIdAndExpiryDateBefore(Long hospitalId, LocalDate expiryDate);

    @Query("select m from MedicineStock m where m.hospital.id = :hospitalId and m.medicineName = :medicineName " +
           "and m.expiryDate > CURRENT_DATE and (m.availableQuantity - m.quantityReserved) > 0 " +
           "and not exists (select 1 from PharmacyRecall r where r.hospital.id = m.hospital.id and r.medicineName = m.medicineName and r.batchNumber = m.batchNumber and r.active = true) " +
           "order by m.expiryDate asc")
    List<MedicineStock> findValidBatchesFifo(@Param("hospitalId") Long hospitalId,
                                              @Param("medicineName") String medicineName);

    @Query("select m from MedicineStock m where m.hospital.id = :hospitalId and m.medicineName = :medicineName " +
           "and m.expiryDate > CURRENT_DATE and (m.availableQuantity - m.quantityReserved) > 0 " +
           "and not exists (select 1 from PharmacyRecall r where r.hospital.id = m.hospital.id and r.medicineName = m.medicineName and r.batchNumber = m.batchNumber and r.active = true) " +
           "order by m.expiryDate asc")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<MedicineStock> findValidBatchesFifoLocked(@Param("hospitalId") Long hospitalId,
                                                    @Param("medicineName") String medicineName);

    @Query("select distinct m.medicineName from MedicineStock m where m.hospital.id = :hospitalId " +
           "and m.expiryDate > CURRENT_DATE and (m.availableQuantity - m.quantityReserved) > 0")
    List<String> findAvailableMedicineNames(@Param("hospitalId") Long hospitalId);
}
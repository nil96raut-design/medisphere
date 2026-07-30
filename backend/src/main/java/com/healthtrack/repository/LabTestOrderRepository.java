package com.healthtrack.repository;

import com.healthtrack.entity.LabOrderStatus;
import com.healthtrack.entity.LabTestOrder;
import com.healthtrack.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface LabTestOrderRepository extends JpaRepository<LabTestOrder, Long> {

    List<LabTestOrder> findByStatusOrderByCreatedAtDesc(LabOrderStatus status);

    long countByStatus(LabOrderStatus status);

    List<LabTestOrder> findByPatientIdOrderByCreatedAtDesc(Long patientId);

    Page<LabTestOrder> findByHospitalIdAndStatusOrderByCreatedAtDesc(
            Long hospitalId, LabOrderStatus status, Pageable pageable);

    long countByHospitalIdAndStatus(Long hospitalId, LabOrderStatus status);

    @Query("select count(l) from LabTestOrder l where l.hospital.id = :hospitalId and l.createdAt between :start and :end")
    long countByHospitalIdAndCreatedAtBetween(
            @Param("hospitalId") Long hospitalId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("select count(l) from LabTestOrder l where l.hospital.id = :hospitalId and l.status = :status and l.completedAt between :start and :end")
    long countByHospitalIdAndStatusUpdatedAtBetween(
            @Param("hospitalId") Long hospitalId,
            @Param("status") LabOrderStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("select l from LabTestOrder l where l.hospital.id = :hospitalId and l.resultValues is not null and upper(l.resultValues) like '%ABNORMAL%'")
    List<LabTestOrder> findAbnormalByHospitalId(@Param("hospitalId") Long hospitalId);

    @Query("select count(l) from LabTestOrder l where l.hospital.id = :hospitalId and l.resultValues is not null and upper(l.resultValues) like '%ABNORMAL%'")
    long countAbnormalByHospitalId(@Param("hospitalId") Long hospitalId);

    long countByRequestedByAndCreatedAtBetween(User requestedBy, LocalDateTime start, LocalDateTime end);

    List<LabTestOrder> findByHospitalIdAndStatusInOrderByCreatedAtDesc(Long hospitalId, List<LabOrderStatus> statuses);

    Page<LabTestOrder> findByHospitalIdAndStatusInOrderByCreatedAtDesc(
            Long hospitalId, List<LabOrderStatus> statuses, Pageable pageable);

    List<LabTestOrder> findByHospitalIdAndCriticalFlagTrue(Long hospitalId);

    long countByHospitalIdAndCriticalFlagTrue(Long hospitalId);

    @Query("select l from LabTestOrder l where l.hospital.id = :hospitalId and l.retestOf is not null")
    List<LabTestOrder> findRetestsByHospitalId(@Param("hospitalId") Long hospitalId);

    @Query("select avg(l.turnaroundMinutes) from LabTestOrder l where l.hospital.id = :hospitalId and l.status = 'APPROVED' and l.turnaroundMinutes is not null")
    Double avgTurnaroundByHospitalId(@Param("hospitalId") Long hospitalId);

    @Query("select l from LabTestOrder l where l.hospital.id = :hospitalId and l.sampleBarcode = :barcode")
    List<LabTestOrder> findByHospitalIdAndSampleBarcode(@Param("hospitalId") Long hospitalId, @Param("barcode") String barcode);

    @Query("select count(l) from LabTestOrder l where l.hospital.id = :hospitalId and l.status in :statuses and l.createdAt between :start and :end")
    long countByHospitalIdAndStatusInAndCreatedAtBetween(
            @Param("hospitalId") Long hospitalId,
            @Param("statuses") List<LabOrderStatus> statuses,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}

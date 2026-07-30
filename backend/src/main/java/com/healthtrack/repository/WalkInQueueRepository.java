package com.healthtrack.repository;

import com.healthtrack.entity.Doctor;
import com.healthtrack.entity.WalkInQueue;
import com.healthtrack.entity.WalkInQueueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WalkInQueueRepository extends JpaRepository<WalkInQueue, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM WalkInQueue w WHERE w.doctor.id = :doctorId AND w.createdAt BETWEEN :start AND :end ORDER BY w.tokenNo DESC")
    List<WalkInQueue> findLastTokenLocked(@Param("doctorId") Long doctorId,
                                          @Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end);

    @Query("SELECT w FROM WalkInQueue w JOIN FETCH w.patient JOIN FETCH w.doctor JOIN FETCH w.doctor.user JOIN FETCH w.hospital " +
           "WHERE w.doctor.id = :doctorId AND w.createdAt BETWEEN :start AND :end ORDER BY " +
           "CASE w.priority WHEN 'EMERGENCY' THEN 0 WHEN 'HIGH' THEN 1 ELSE 2 END, w.tokenNo ASC")
    List<WalkInQueue> findQueueByDoctorAndDateOrdered(@Param("doctorId") Long doctorId,
                                                       @Param("start") LocalDateTime start,
                                                       @Param("end") LocalDateTime end);

    long countByDoctorIdAndCreatedAtBetweenAndStatusNot(Long doctorId, LocalDateTime start, LocalDateTime end, WalkInQueueStatus status);

    long countByHospitalIdAndCreatedAtBetween(Long hospitalId, LocalDateTime start, LocalDateTime end);

    long countByHospitalIdAndCreatedAtBetweenAndPriority(Long hospitalId, LocalDateTime start, LocalDateTime end, com.healthtrack.entity.QueuePriority priority);

    long countByHospitalIdAndCreatedAtBetweenAndStatus(Long hospitalId, LocalDateTime start, LocalDateTime end, WalkInQueueStatus status);
}

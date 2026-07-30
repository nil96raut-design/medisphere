package com.healthtrack.repository;

import com.healthtrack.entity.MedicationSchedule;
import com.healthtrack.entity.MedicationScheduleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface MedicationScheduleRepository extends JpaRepository<MedicationSchedule, Long> {

    List<MedicationSchedule> findByPatientIdOrderByScheduledTimeAsc(Long patientId);

    List<MedicationSchedule> findByPrescriptionItemIdOrderByScheduledTimeAsc(Long prescriptionItemId);

    List<MedicationSchedule> findByStatusAndScheduledTimeBefore(MedicationScheduleStatus status, OffsetDateTime now);

    Optional<MedicationSchedule> findByPrescriptionItemIdAndScheduledTime(Long prescriptionItemId, OffsetDateTime scheduledTime);

    boolean existsByPrescriptionItemIdAndScheduledTime(Long prescriptionItemId, OffsetDateTime scheduledTime);

    long countByPrescriptionItemIdAndStatus(Long prescriptionItemId, MedicationScheduleStatus status);

    @Query("SELECT ms FROM MedicationSchedule ms WHERE ms.prescriptionItemId = :pid AND ms.status = 'PENDING' ORDER BY ms.scheduledTime ASC")
    List<MedicationSchedule> findPendingByPrescriptionItemId(@Param("pid") Long prescriptionItemId);
}

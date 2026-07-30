package com.healthtrack.repository;

import com.healthtrack.entity.NurseTask;
import com.healthtrack.entity.NurseTaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface NurseTaskRepository extends JpaRepository<NurseTask, Long> {

    List<NurseTask> findByNurseIdOrderByCreatedAtDesc(Long nurseId);

    List<NurseTask> findByPatientIdOrderByCreatedAtDesc(Long patientId);

    List<NurseTask> findByNurseIdAndStatusOrderByDueTimeAsc(Long nurseId, NurseTaskStatus status);

    long countByNurseIdAndStatus(Long nurseId, NurseTaskStatus status);

    @Query("SELECT nt FROM NurseTask nt JOIN FETCH nt.patient JOIN FETCH nt.nurse " +
           "WHERE nt.nurse.id = :nurseId ORDER BY nt.priority DESC, nt.dueTime ASC, nt.createdAt ASC")
    List<NurseTask> findByNurseIdFetching(@Param("nurseId") Long nurseId);

    List<NurseTask> findByIsRecurringTrueAndStatus(NurseTaskStatus status);

    @Query("SELECT nt FROM NurseTask nt WHERE nt.status = 'PENDING' AND nt.dueTime < :cutoff")
    List<NurseTask> findOverdueTasks(@Param("cutoff") OffsetDateTime cutoff);
}

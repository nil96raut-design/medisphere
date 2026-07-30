package com.healthtrack.repository;

import com.healthtrack.entity.NurseAssignment;
import com.healthtrack.entity.NurseAssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NurseAssignmentRepository extends JpaRepository<NurseAssignment, Long> {

    Optional<NurseAssignment> findByPatientIdAndStatus(Long patientId, NurseAssignmentStatus status);

    boolean existsByPatientIdAndStatus(Long patientId, NurseAssignmentStatus status);

    List<NurseAssignment> findByNurseIdAndStatus(Long nurseId, NurseAssignmentStatus status);

    @Query("SELECT na FROM NurseAssignment na JOIN FETCH na.patient JOIN FETCH na.nurse " +
           "LEFT JOIN FETCH na.bed " +
           "WHERE na.nurse.id = :nurseId AND na.status = :status")
    List<NurseAssignment> findByNurseIdAndStatusFetching(@Param("nurseId") Long nurseId,
                                                          @Param("status") NurseAssignmentStatus status);

    long countByNurseIdAndStatus(Long nurseId, NurseAssignmentStatus status);
}

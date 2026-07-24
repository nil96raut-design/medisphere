package com.healthtrack.repository;

import com.healthtrack.entity.Admission;
import com.healthtrack.entity.AdmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdmissionRepository extends JpaRepository<Admission, Long> {

    List<Admission> findByPatientIdOrderByAdmissionDateDesc(Long patientId);

    List<Admission> findByStatusOrderByAdmissionDateDesc(AdmissionStatus status);

    long countByStatus(AdmissionStatus status);
}

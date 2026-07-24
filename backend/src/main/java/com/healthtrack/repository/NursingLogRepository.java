package com.healthtrack.repository;

import com.healthtrack.entity.NursingLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NursingLogRepository extends JpaRepository<NursingLog, Long> {

    List<NursingLog> findByAdmissionIdOrderByLoggedAtDesc(Long admissionId);
}

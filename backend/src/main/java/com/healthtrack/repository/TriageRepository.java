package com.healthtrack.repository;

import com.healthtrack.entity.Triage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TriageRepository extends JpaRepository<Triage, Long> {
    List<Triage> findByPatientIdOrderByRecordedAtDesc(Long patientId);
}

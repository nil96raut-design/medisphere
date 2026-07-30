package com.healthtrack.repository;

import com.healthtrack.entity.NursingNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NursingNoteRepository extends JpaRepository<NursingNote, Long> {

    List<NursingNote> findByPatientIdOrderByCreatedAtDesc(Long patientId);
}

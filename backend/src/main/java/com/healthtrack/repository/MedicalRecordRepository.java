package com.healthtrack.repository;

import com.healthtrack.entity.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {

    List<MedicalRecord> findByPatientIdOrderByEncounterDateDesc(Long patientId);

    long countByDoctorIdAndEncounterDate(Long doctorId, LocalDate encounterDate);
}

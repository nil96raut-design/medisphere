package com.healthtrack.repository;

import com.healthtrack.entity.MedicationAdministration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MedicationAdministrationRepository extends JpaRepository<MedicationAdministration, Long> {

    Optional<MedicationAdministration> findByPrescriptionItemId(Long prescriptionItemId);

    boolean existsByPrescriptionItemId(Long prescriptionItemId);

    List<MedicationAdministration> findByPatientIdOrderByAdministeredAtDesc(Long patientId);
}

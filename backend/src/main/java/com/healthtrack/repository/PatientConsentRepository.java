package com.healthtrack.repository;

import com.healthtrack.entity.PatientConsent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientConsentRepository extends JpaRepository<PatientConsent, Long> {

    List<PatientConsent> findByPatientId(Long patientId);

    Optional<PatientConsent> findByPatientIdAndGrantedRoleAndConsentType(Long patientId, String grantedRole, String consentType);
}

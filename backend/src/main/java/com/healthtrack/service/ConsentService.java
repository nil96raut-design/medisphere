package com.healthtrack.service;

import com.healthtrack.entity.PatientConsent;
import com.healthtrack.repository.PatientConsentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConsentService {

    private final PatientConsentRepository consentRepository;

    @Transactional(readOnly = true)
    public boolean hasConsent(Long patientId, String role, String consentType) {
        return consentRepository.findByPatientIdAndGrantedRoleAndConsentType(patientId, role, consentType)
                .map(c -> Boolean.TRUE.equals(c.getIsGranted()) &&
                        (c.getExpiresAt() == null || c.getExpiresAt().isAfter(OffsetDateTime.now())))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public List<PatientConsent> getPatientConsents(Long patientId) {
        return consentRepository.findByPatientId(patientId);
    }

    @Transactional
    public PatientConsent updateConsent(Long patientId, String role, String consentType, boolean isGranted, String notes) {
        PatientConsent consent = consentRepository.findByPatientIdAndGrantedRoleAndConsentType(patientId, role, consentType)
                .orElseGet(() -> PatientConsent.builder()
                        .patientId(patientId)
                        .grantedRole(role)
                        .consentType(consentType)
                        .build());

        consent.setIsGranted(isGranted);
        consent.setNotes(notes);
        return consentRepository.save(consent);
    }
}

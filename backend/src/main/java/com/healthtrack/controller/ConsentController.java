package com.healthtrack.controller;

import com.healthtrack.entity.PatientConsent;
import com.healthtrack.service.ConsentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/consent")
@RequiredArgsConstructor
public class ConsentController {

    private final ConsentService consentService;

    @GetMapping("/patients/{patientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'NURSE')")
    public ResponseEntity<List<PatientConsent>> getConsents(@PathVariable Long patientId) {
        return ResponseEntity.ok(consentService.getPatientConsents(patientId));
    }

    @PostMapping("/patients/{patientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FRONTDESK')")
    public ResponseEntity<PatientConsent> updateConsent(
            @PathVariable Long patientId,
            @RequestParam String role,
            @RequestParam String consentType,
            @RequestParam boolean isGranted,
            @RequestParam(required = false) String notes) {
        return ResponseEntity.ok(consentService.updateConsent(patientId, role, consentType, isGranted, notes));
    }
}

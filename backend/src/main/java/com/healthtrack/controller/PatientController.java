package com.healthtrack.controller;

import com.healthtrack.dto.PatientDtos.PatientRegistrationRequest;
import com.healthtrack.dto.PatientDtos.PatientResponse;
import com.healthtrack.security.UserPrincipal;
import com.healthtrack.service.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    @PostMapping
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'ADMIN', 'DOCTOR')")
    public ResponseEntity<PatientResponse> registerPatient(
            @Valid @RequestBody PatientRegistrationRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(patientService.registerPatient(request, currentUser));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'ADMIN', 'DOCTOR', 'NURSE')")
    public ResponseEntity<PatientResponse> getPatient(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(patientService.getPatient(id, currentUser));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'ADMIN', 'DOCTOR', 'NURSE')")
    public ResponseEntity<Page<PatientResponse>> searchPatients(
            @RequestParam(required = false) String q,
            Pageable pageable,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(patientService.searchPatients(q, pageable, currentUser));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'ADMIN', 'DOCTOR')")
    public ResponseEntity<PatientResponse> updatePatient(
            @PathVariable Long id,
            @Valid @RequestBody PatientRegistrationRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(patientService.updatePatient(id, request, currentUser));
    }
}

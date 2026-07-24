package com.healthtrack.controller;

import com.healthtrack.dto.PatientDtos.PatientRegistrationRequest;
import com.healthtrack.dto.PatientDtos.PatientResponse;
import com.healthtrack.security.UserPrincipal;
import com.healthtrack.service.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'ADMIN', 'DOCTOR', 'NURSE')")
    public ResponseEntity<List<PatientResponse>> searchPatients(
            @RequestParam(required = false) String q,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(patientService.searchPatients(q, currentUser));
    }
}

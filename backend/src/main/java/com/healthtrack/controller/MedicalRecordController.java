package com.healthtrack.controller;

import com.healthtrack.dto.EmrDtos.CreateMedicalRecordRequest;
import com.healthtrack.dto.EmrDtos.MedicalRecordResponse;
import com.healthtrack.security.UserPrincipal;
import com.healthtrack.service.MedicalRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;

    @GetMapping("/patients/{patientId}/history")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE', 'ADMIN')")
    public ResponseEntity<List<MedicalRecordResponse>> getPatientHistory(
            @PathVariable Long patientId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(medicalRecordService.getPatientHistory(patientId, currentUser));
    }

    @PostMapping("/medical-records")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<MedicalRecordResponse> createMedicalRecord(
            @Valid @RequestBody CreateMedicalRecordRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(medicalRecordService.createRecord(request, currentUser));
    }
}

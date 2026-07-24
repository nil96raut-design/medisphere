package com.healthtrack.controller;

import com.healthtrack.dto.IpdDtos.*;
import com.healthtrack.security.UserPrincipal;
import com.healthtrack.service.IpdService;
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
public class IpdController {

    private final IpdService ipdService;

    @GetMapping("/beds/available")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE', 'RECEPTIONIST', 'ADMIN')")
    public ResponseEntity<List<BedResponse>> getAvailableBeds(@AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ipdService.getAvailableBeds(currentUser));
    }

    @PostMapping("/admissions")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<AdmissionResponse> admitPatient(
            @Valid @RequestBody AdmissionRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ipdService.admitPatient(request, currentUser));
    }

    @PostMapping("/admissions/{id}/nursing-log")
    @PreAuthorize("hasAnyRole('NURSE', 'DOCTOR', 'ADMIN')")
    public ResponseEntity<NursingLogResponse> addNursingLog(
            @PathVariable Long id,
            @Valid @RequestBody NursingLogRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ipdService.addNursingLog(id, request, currentUser));
    }

    @PostMapping("/admissions/{id}/discharge")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<AdmissionResponse> discharge(
            @PathVariable Long id,
            @Valid @RequestBody DischargeRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ipdService.discharge(id, request, currentUser));
    }

    @GetMapping("/admissions/active")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE', 'ADMIN')")
    public ResponseEntity<List<AdmissionResponse>> getActiveAdmissions(@AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ipdService.getActiveAdmissions(currentUser));
    }
}

package com.healthtrack.controller;

import com.healthtrack.dto.PatientDtos.TriageLogRequest;
import com.healthtrack.dto.PatientDtos.TriageResponse;
import com.healthtrack.security.UserPrincipal;
import com.healthtrack.service.TriageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class TriageController {

    private final TriageService triageService;

    @PostMapping("/{id}/triage")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'ADMIN', 'DOCTOR', 'NURSE')")
    public ResponseEntity<TriageResponse> logVitals(
            @PathVariable Long id,
            @Valid @RequestBody TriageLogRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(triageService.logVitals(id, request, currentUser));
    }
}

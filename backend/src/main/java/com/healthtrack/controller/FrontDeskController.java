package com.healthtrack.controller;

import com.healthtrack.dto.FrontDeskDtos.*;
import com.healthtrack.dto.IpdDtos.AdmissionResponse;
import com.healthtrack.dto.BillingDtos.BillResponse;
import com.healthtrack.security.UserPrincipal;
import com.healthtrack.service.FrontDeskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/frontdesk")
@RequiredArgsConstructor
public class FrontDeskController {

    private final FrontDeskService frontDeskService;

    @PostMapping("/walk-in")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'ADMIN')")
    public ResponseEntity<WalkInResponse> addWalkIn(
            @Valid @RequestBody WalkInRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(frontDeskService.addWalkIn(request, currentUser));
    }

    @GetMapping("/queue")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'ADMIN', 'DOCTOR')")
    public ResponseEntity<List<QueueEntry>> getQueue(
            @RequestParam Long doctorId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(frontDeskService.getQueue(doctorId, currentUser));
    }

    @PatchMapping("/queue/{id}/status")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'ADMIN', 'DOCTOR')")
    public ResponseEntity<WalkInResponse> updateQueueStatus(
            @PathVariable Long id,
            @Valid @RequestBody QueueStatusUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(frontDeskService.updateQueueStatus(id, request, currentUser));
    }

    @GetMapping("/patients/check-duplicate")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'ADMIN', 'DOCTOR')")
    public ResponseEntity<DuplicateCheckResponse> checkDuplicate(
            @Valid DuplicateCheckRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(frontDeskService.checkDuplicatePatient(request, currentUser));
    }

    @PostMapping("/admissions/initiate")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'ADMIN')")
    public ResponseEntity<AdmissionResponse> initiateAdmission(
            @Valid @RequestBody ProvisionalAdmissionRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(frontDeskService.initiateProvisionalAdmission(request, currentUser));
    }

    @PostMapping("/billing/initiate")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'ADMIN')")
    public ResponseEntity<BillResponse> initiateBilling(
            @Valid @RequestBody BillingInitiateRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(frontDeskService.initiateBilling(request, currentUser));
    }

    @GetMapping("/daily-summary")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'ADMIN')")
    public ResponseEntity<DailySummaryResponse> getDailySummary(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(frontDeskService.getDailySummary(currentUser));
    }
}

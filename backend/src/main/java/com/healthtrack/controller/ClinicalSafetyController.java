package com.healthtrack.controller;

import com.healthtrack.dto.ClinicalSafetyDtos.*;
import com.healthtrack.dto.NurseDtos.VitalTrendResponse;
import com.healthtrack.security.UserPrincipal;
import com.healthtrack.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ClinicalSafetyController {

    private final MedicationSchedulingService medicationSchedulingService;
    private final VitalTrendService vitalTrendService;
    private final AlertService alertService;
    private final ShiftHandoverService shiftHandoverService;
    private final BedCleaningService bedCleaningService;

    // ──────────────────────────────────────────────
    // MEDICATION SCHEDULES
    // ──────────────────────────────────────────────

    @GetMapping("/api/medication-schedules/patient/{patientId}")
    @PreAuthorize("hasAnyRole('NURSE', 'DOCTOR')")
    public ResponseEntity<List<MedicationScheduleResponse>> getPatientSchedules(@PathVariable Long patientId) {
        return ResponseEntity.ok(medicationSchedulingService.getPatientSchedules(patientId));
    }

    @GetMapping("/api/medication-schedules/patient/{patientId}/pending")
    @PreAuthorize("hasAnyRole('NURSE', 'DOCTOR')")
    public ResponseEntity<List<MedicationScheduleResponse>> getPendingSchedules(@PathVariable Long patientId) {
        return ResponseEntity.ok(medicationSchedulingService.getPendingSchedules(patientId));
    }

    @PostMapping("/api/medication-schedules/{id}/mark-given")
    @PreAuthorize("hasRole('NURSE')")
    public ResponseEntity<MedicationScheduleResponse> markScheduleGiven(@PathVariable Long id,
                                                                         @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(medicationSchedulingService.markGiven(id, currentUser.getUser().getId()));
    }

    @PostMapping("/api/prescription-items/{id}/generate-schedules")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<List<MedicationScheduleResponse>> generateSchedules(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(medicationSchedulingService.generateSchedules(id));
    }

    // ──────────────────────────────────────────────
    // VITAL TRENDS
    // ──────────────────────────────────────────────

    @GetMapping("/api/vitals/trend")
    @PreAuthorize("hasAnyRole('NURSE', 'DOCTOR')")
    public ResponseEntity<VitalTrendResponse> getVitalTrend(@RequestParam Long patientId) {
        return ResponseEntity.ok(vitalTrendService.getTrend(patientId));
    }

    // ──────────────────────────────────────────────
    // ALERTS
    // ──────────────────────────────────────────────

    @GetMapping("/api/alerts/active")
    @PreAuthorize("hasAnyRole('NURSE', 'DOCTOR', 'ADMIN')")
    public ResponseEntity<List<AlertResponse>> getActiveAlerts(@AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(alertService.getActiveAlerts(currentUser.getHospitalId()));
    }

    @GetMapping("/api/alerts/patient/{patientId}")
    @PreAuthorize("hasAnyRole('NURSE', 'DOCTOR', 'ADMIN')")
    public ResponseEntity<List<AlertResponse>> getPatientAlerts(@PathVariable Long patientId,
                                                                 @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(alertService.getPatientAlerts(patientId, currentUser));
    }

    @PostMapping("/api/alerts/{id}/acknowledge")
    @PreAuthorize("hasAnyRole('NURSE', 'DOCTOR')")
    public ResponseEntity<AlertResponse> acknowledgeAlert(@PathVariable Long id,
                                                          @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(alertService.acknowledgeAlert(id, currentUser));
    }

    @PostMapping("/api/alerts/{id}/resolve")
    @PreAuthorize("hasAnyRole('NURSE', 'DOCTOR', 'ADMIN')")
    public ResponseEntity<AlertResponse> resolveAlert(@PathVariable Long id,
                                                       @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(alertService.resolveAlert(id, currentUser));
    }

    // ──────────────────────────────────────────────
    // SHIFT HANDOVER
    // ──────────────────────────────────────────────

    @PostMapping("/api/nurse/handover")
    @PreAuthorize("hasRole('NURSE')")
    public ResponseEntity<ShiftHandoverResponse> submitHandover(@Valid @RequestBody ShiftHandoverRequest request,
                                                                @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(shiftHandoverService.submitHandover(request, currentUser));
    }

    @GetMapping("/api/nurse/handovers")
    @PreAuthorize("hasRole('NURSE')")
    public ResponseEntity<List<ShiftHandoverResponse>> getMyHandovers(@AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(shiftHandoverService.getMyHandovers(currentUser));
    }

    // ──────────────────────────────────────────────
    // BED CLEANING
    // ──────────────────────────────────────────────

    @PostMapping("/api/beds/{bedId}/request-cleaning")
    @PreAuthorize("hasAnyRole('NURSE', 'ADMIN')")
    public ResponseEntity<BedCleaningResponse> requestCleaning(@PathVariable Long bedId,
                                                                @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bedCleaningService.requestCleaning(bedId, currentUser));
    }

    @PatchMapping("/api/bed-cleaning/{cleaningId}/mark-cleaned")
    @PreAuthorize("hasAnyRole('NURSE', 'ADMIN')")
    public ResponseEntity<BedCleaningResponse> markCleaned(@PathVariable Long cleaningId,
                                                            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(bedCleaningService.markCleaned(cleaningId, currentUser));
    }

    @GetMapping("/api/bed-cleaning/pending")
    @PreAuthorize("hasAnyRole('NURSE', 'ADMIN')")
    public ResponseEntity<List<BedCleaningResponse>> getPendingCleaning(@AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(bedCleaningService.getPendingCleaningRequests(currentUser));
    }
}

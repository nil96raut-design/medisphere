package com.healthtrack.controller;

import com.healthtrack.dto.NurseDtos.*;
import com.healthtrack.entity.NurseTaskStatus;
import com.healthtrack.security.UserPrincipal;
import com.healthtrack.service.NurseService;
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
public class NurseController {

    private final NurseService nurseService;
    private final com.healthtrack.service.IdempotencyService idempotencyService;

    @PostMapping("/api/nurse/assignments")
    @PreAuthorize("hasRole('NURSE')")
    public ResponseEntity<NurseAssignmentResponse> assignNurse(@Valid @RequestBody AssignNurseRequest request,
                                                                @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(nurseService.assignNurse(request, currentUser));
    }

    @GetMapping("/api/nurse/patients")
    @PreAuthorize("hasRole('NURSE')")
    public ResponseEntity<List<AssignedPatientResponse>> getAssignedPatients(@AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(nurseService.getAssignedPatients(currentUser));
    }

    @PostMapping("/api/nurse/assignments/{id}/release")
    @PreAuthorize("hasRole('NURSE')")
    public ResponseEntity<Void> releaseAssignment(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal currentUser) {
        nurseService.releaseAssignment(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/vitals")
    @PreAuthorize("hasRole('NURSE')")
    public ResponseEntity<VitalRecordResponse> recordVitals(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody VitalRecordRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            if (!idempotencyService.tryProcess(idempotencyKey, currentUser.getHospitalId(), "RECORD_VITALS")) {
                throw new org.springframework.web.server.ResponseStatusException(HttpStatus.CONFLICT, "Duplicate request");
            }
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(nurseService.recordVitals(request, currentUser));
    }

    @GetMapping("/api/vitals/patient/{patientId}")
    @PreAuthorize("hasAnyRole('NURSE', 'DOCTOR')")
    public ResponseEntity<List<VitalRecordResponse>> getVitals(@PathVariable Long patientId,
                                                               @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(nurseService.getVitals(patientId, currentUser));
    }

    @PostMapping("/api/medication-administration")
    @PreAuthorize("hasRole('NURSE')")
    public ResponseEntity<MedicationAdminResponse> administerMedication(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody MedicationAdminRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            if (!idempotencyService.tryProcess(idempotencyKey, currentUser.getHospitalId(), "ADMINISTER_MEDICATION")) {
                throw new org.springframework.web.server.ResponseStatusException(HttpStatus.CONFLICT, "Duplicate request");
            }
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(nurseService.administerMedication(request, currentUser));
    }

    @GetMapping("/api/medication-administration/patient/{patientId}")
    @PreAuthorize("hasAnyRole('NURSE', 'DOCTOR')")
    public ResponseEntity<List<MedicationAdminResponse>> getMedicationHistory(@PathVariable Long patientId,
                                                                              @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(nurseService.getMedicationHistory(patientId, currentUser));
    }

    @PostMapping("/api/nursing-notes")
    @PreAuthorize("hasRole('NURSE')")
    public ResponseEntity<NursingNoteResponse> addNursingNote(@Valid @RequestBody NursingNoteRequest request,
                                                              @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(nurseService.addNursingNote(request, currentUser));
    }

    @GetMapping("/api/nursing-notes/patient/{patientId}")
    @PreAuthorize("hasAnyRole('NURSE', 'DOCTOR')")
    public ResponseEntity<List<NursingNoteResponse>> getNursingNotes(@PathVariable Long patientId,
                                                                     @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(nurseService.getNursingNotes(patientId, currentUser));
    }

    @GetMapping("/api/nurse/tasks")
    @PreAuthorize("hasRole('NURSE')")
    public ResponseEntity<List<NurseTaskResponse>> getTasks(@AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(nurseService.getTasks(currentUser));
    }

    @PutMapping("/api/nurse/tasks/{id}/status")
    @PreAuthorize("hasRole('NURSE')")
    public ResponseEntity<NurseTaskResponse> updateTaskStatus(@PathVariable Long id,
                                                              @RequestBody NurseTaskStatus status,
                                                              @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(nurseService.updateTaskStatus(id, status, currentUser));
    }

    @PostMapping("/api/nurse/tasks")
    @PreAuthorize("hasRole('NURSE')")
    public ResponseEntity<NurseTaskResponse> createTask(@Valid @RequestBody NurseTaskRequest request,
                                                        @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(nurseService.createTask(request, currentUser));
    }
}

package com.healthtrack.controller;

import com.healthtrack.dto.AppointmentDtos.AppointmentResponse;
import com.healthtrack.dto.AppointmentDtos.DoctorResponse;
import com.healthtrack.dto.AppointmentDtos.StatusUpdateRequest;
import com.healthtrack.dto.DoctorDtos.DoctorStatsResponse;
import com.healthtrack.dto.DoctorDtos.PatientFullProfileResponse;
import com.healthtrack.dto.DoctorDtos.TodayScheduleResponse;
import com.healthtrack.security.UserPrincipal;
import com.healthtrack.service.AppointmentService;
import com.healthtrack.service.DoctorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService doctorService;
    private final AppointmentService appointmentService;

    @GetMapping("/available")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'ADMIN', 'DOCTOR', 'NURSE')")
    public ResponseEntity<List<DoctorResponse>> getAvailableDoctors(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(doctorService.getAvailableDoctors(currentUser));
    }

    @GetMapping("/schedule")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<List<TodayScheduleResponse>> getTodaySchedule(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(doctorService.getTodaySchedule(currentUser));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<DoctorStatsResponse> getStats(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(doctorService.getStats(currentUser));
    }

    @GetMapping("/patients/{patientId}/full-profile")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<PatientFullProfileResponse> getPatientFullProfile(
            @PathVariable Long patientId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(doctorService.getPatientFullProfile(patientId, currentUser));
    }

    @PostMapping("/appointments/{id}/status")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<AppointmentResponse> updateAppointmentStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(appointmentService.updateStatus(id, request, currentUser));
    }
}

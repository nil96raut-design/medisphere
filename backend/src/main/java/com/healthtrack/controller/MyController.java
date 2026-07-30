package com.healthtrack.controller;

import com.healthtrack.dto.AppointmentDtos.AppointmentRequest;
import com.healthtrack.dto.AppointmentDtos.AppointmentResponse;
import com.healthtrack.dto.AppointmentDtos.DoctorResponse;
import com.healthtrack.dto.BillingDtos.SettleRequest;
import com.healthtrack.dto.BillingDtos.CalculateResponse;
import com.healthtrack.dto.DoctorDtos.PatientFullProfileResponse;
import com.healthtrack.dto.LabDtos.LabOrderResponse;
import com.healthtrack.security.UserPrincipal;
import com.healthtrack.service.PatientPortalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/my")
@RequiredArgsConstructor
public class MyController {

    private final PatientPortalService patientPortalService;

    @GetMapping("/profile")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<PatientFullProfileResponse> getMyProfile(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(patientPortalService.getMyProfile(currentUser));
    }

    @GetMapping("/appointments")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<Page<AppointmentResponse>> getMyAppointments(
            Pageable pageable,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(patientPortalService.getMyAppointments(pageable, currentUser));
    }

    @PostMapping("/appointments")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<AppointmentResponse> bookMyAppointment(
            @Valid @RequestBody AppointmentRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(patientPortalService.bookMyAppointment(request, currentUser));
    }

    @GetMapping("/medical-records")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<List<com.healthtrack.dto.EmrDtos.MedicalRecordResponse>> getMyMedicalRecords(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(patientPortalService.getMyMedicalRecords(currentUser));
    }

    @GetMapping("/lab-orders")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<List<LabOrderResponse>> getMyLabOrders(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(patientPortalService.getMyLabOrders(currentUser));
    }

    @GetMapping("/bills")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<List<com.healthtrack.dto.BillingDtos.BillResponse>> getMyBills(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(patientPortalService.getMyBills(currentUser));
    }

    @GetMapping("/doctors")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<List<DoctorResponse>> getAvailableDoctors(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(patientPortalService.getAvailableDoctors(currentUser));
    }
}

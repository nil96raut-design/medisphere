package com.healthtrack.controller;

import com.healthtrack.dto.AppointmentDtos.DoctorResponse;
import com.healthtrack.security.UserPrincipal;
import com.healthtrack.service.DoctorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService doctorService;

    @GetMapping("/available")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'ADMIN', 'DOCTOR', 'NURSE')")
    public ResponseEntity<List<DoctorResponse>> getAvailableDoctors(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(doctorService.getAvailableDoctors(currentUser));
    }
}

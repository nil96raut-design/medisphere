package com.healthtrack.controller;

import com.healthtrack.dto.AppointmentDtos.QueueEntry;
import com.healthtrack.security.UserPrincipal;
import com.healthtrack.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/queue")
@RequiredArgsConstructor
public class QueueController {

    private final AppointmentService appointmentService;

    @GetMapping("/doctor/{doctorId}")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'ADMIN', 'DOCTOR')")
    public ResponseEntity<List<QueueEntry>> getQueue(
            @PathVariable Long doctorId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(appointmentService.getQueue(doctorId, currentUser));
    }
}

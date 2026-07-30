package com.healthtrack.controller;

import com.healthtrack.dto.TimelineEventDto;
import com.healthtrack.security.UserPrincipal;
import com.healthtrack.service.TimelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patients/{patientId}/timeline")
@RequiredArgsConstructor
public class TimelineController {

    private final TimelineService timelineService;

    @GetMapping
    public ResponseEntity<List<TimelineEventDto>> getPatientTimeline(
            @PathVariable Long patientId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(timelineService.getPatientTimeline(patientId, currentUser));
    }
}

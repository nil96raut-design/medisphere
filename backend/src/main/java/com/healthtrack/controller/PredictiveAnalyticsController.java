package com.healthtrack.controller;

import com.healthtrack.security.UserPrincipal;
import com.healthtrack.service.PredictiveAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics/predictive")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class PredictiveAnalyticsController {

    private final PredictiveAnalyticsService predictiveAnalyticsService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getPredictiveMetrics(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(predictiveAnalyticsService.getPredictiveMetrics(currentUser.getHospitalId()));
    }
}

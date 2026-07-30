package com.healthtrack.controller;

import com.healthtrack.service.AnalyticsService;
import com.healthtrack.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/revenue")
    public ResponseEntity<Map<String, Object>> getRevenue(
            @RequestParam(defaultValue = "12") int months,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(analyticsService.getRevenueAnalytics(currentUser.getHospitalId(), months));
    }

    @GetMapping("/top-doctors")
    public ResponseEntity<List<Map<String, Object>>> getTopDoctors(
            @RequestParam(defaultValue = "10") int limit,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(analyticsService.getTopDoctors(currentUser.getHospitalId(), limit));
    }

    @GetMapping("/lab-volume")
    public ResponseEntity<Map<String, Object>> getLabVolume(
            @RequestParam(defaultValue = "6") int months,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(analyticsService.getLabVolume(currentUser.getHospitalId(), months));
    }

    @GetMapping("/pharmacy-sales")
    public ResponseEntity<Map<String, Object>> getPharmacySales(
            @RequestParam(defaultValue = "6") int months,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(analyticsService.getPharmacySales(currentUser.getHospitalId(), months));
    }
}

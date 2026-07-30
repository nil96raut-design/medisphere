package com.healthtrack.controller;

import com.healthtrack.entity.FeatureFlag;
import com.healthtrack.service.FeatureFlagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/feature-flags")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class FeatureFlagController {

    private final FeatureFlagService featureFlagService;

    @GetMapping
    public ResponseEntity<List<FeatureFlag>> getAllFlags() {
        return ResponseEntity.ok(featureFlagService.getAllFeatureFlags());
    }

    @PutMapping("/{featureName}")
    public ResponseEntity<FeatureFlag> toggleFlag(
            @PathVariable String featureName,
            @RequestParam boolean enabled) {
        return ResponseEntity.ok(featureFlagService.toggleFeature(featureName, enabled));
    }
}

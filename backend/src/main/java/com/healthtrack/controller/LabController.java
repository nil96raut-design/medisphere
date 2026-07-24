package com.healthtrack.controller;

import com.healthtrack.dto.LabDtos.*;
import com.healthtrack.security.UserPrincipal;
import com.healthtrack.service.LabReportService;
import com.healthtrack.service.LabService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lab")
@RequiredArgsConstructor
public class LabController {

    private final LabService labService;
    private final LabReportService labReportService;

    @GetMapping("/orders")
    @PreAuthorize("hasAnyRole('LAB_TECH', 'DOCTOR', 'ADMIN')")
    public ResponseEntity<List<LabOrderResponse>> getOrders(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(labService.getOrdersByStatus(status, page, Math.min(size, 100), currentUser));
    }

    @PutMapping("/orders/{id}/sample")
    @PreAuthorize("hasAnyRole('LAB_TECH', 'ADMIN')")
    public ResponseEntity<LabOrderResponse> markSampleCollected(
            @PathVariable Long id,
            @Valid @RequestBody SampleCollectionRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(labService.markSampleCollected(id, request, currentUser));
    }

    @PutMapping("/orders/{id}/results")
    @PreAuthorize("hasAnyRole('LAB_TECH', 'ADMIN')")
    public ResponseEntity<LabOrderResponse> enterResults(
            @PathVariable Long id,
            @Valid @RequestBody ResultEntryRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(labService.enterResults(id, request, currentUser));
    }

    @GetMapping("/orders/{id}/report")
    @PreAuthorize("hasAnyRole('LAB_TECH', 'DOCTOR', 'ADMIN')")
    public ResponseEntity<byte[]> getReport(@PathVariable Long id,
                                              @AuthenticationPrincipal UserPrincipal currentUser) {
        return labReportService.generateReport(id, currentUser);
    }
}

package com.healthtrack.controller;

import com.healthtrack.dto.LabDtos.*;
import com.healthtrack.security.UserPrincipal;
import com.healthtrack.service.LabReportService;
import com.healthtrack.service.LabService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    @GetMapping("/queue")
    @PreAuthorize("hasAnyRole('LAB_TECH', 'ADMIN')")
    public ResponseEntity<LabTechQueueResponse> getQueue(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(labService.getTechQueue(currentUser));
    }

    @GetMapping("/orders")
    @PreAuthorize("hasAnyRole('LAB_TECH', 'DOCTOR', 'ADMIN')")
    public ResponseEntity<List<LabOrderResponse>> getOrders(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(labService.getOrdersByStatus(status, page, Math.min(size, 100), currentUser));
    }

    @GetMapping("/orders/{id}")
    @PreAuthorize("hasAnyRole('LAB_TECH', 'DOCTOR', 'ADMIN')")
    public ResponseEntity<LabOrderResponse> getOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(labService.getOrder(id, currentUser));
    }

    @PutMapping("/orders/{id}/sample")
    @PreAuthorize("hasAnyRole('LAB_TECH', 'NURSE', 'ADMIN')")
    public ResponseEntity<LabOrderResponse> markSampleCollected(
            @PathVariable Long id,
            @Valid @RequestBody SampleCollectionRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(labService.markSampleCollected(id, request, currentUser));
    }

    @PutMapping("/orders/{id}/process")
    @PreAuthorize("hasAnyRole('LAB_TECH', 'ADMIN')")
    public ResponseEntity<LabOrderResponse> startProcessing(
            @PathVariable Long id,
            @Valid @RequestBody ProcessRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(labService.startProcessing(id, request, currentUser));
    }

    @PutMapping("/orders/{id}/results")
    @PreAuthorize("hasAnyRole('LAB_TECH', 'ADMIN')")
    public ResponseEntity<LabOrderResponse> enterResults(
            @PathVariable Long id,
            @Valid @RequestBody ResultEntryRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(labService.enterResults(id, request, currentUser));
    }

    @PutMapping("/orders/{id}/retest")
    @PreAuthorize("hasAnyRole('LAB_TECH', 'DOCTOR', 'ADMIN')")
    public ResponseEntity<LabOrderResponse> requestRetest(
            @PathVariable Long id,
            @Valid @RequestBody RetestRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(labService.requestRetest(id, request, currentUser));
    }

    @PutMapping("/orders/{id}/approve")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<LabOrderResponse> approve(
            @PathVariable Long id,
            @Valid @RequestBody ApproveRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(labService.approve(id, request, currentUser));
    }

    @PutMapping("/orders/{id}/cancel")
    @PreAuthorize("hasAnyRole('LAB_TECH', 'ADMIN')")
    public ResponseEntity<LabOrderResponse> cancel(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(labService.cancel(id, currentUser));
    }

    @GetMapping("/orders/{id}/report")
    @PreAuthorize("hasAnyRole('LAB_TECH', 'DOCTOR', 'ADMIN')")
    public ResponseEntity<byte[]> getReport(@PathVariable Long id,
                                              @AuthenticationPrincipal UserPrincipal currentUser) {
        return labReportService.generateReport(id, currentUser);
    }

    @GetMapping("/metrics")
    @PreAuthorize("hasAnyRole('LAB_TECH', 'ADMIN')")
    public ResponseEntity<LabMetricsResponse> getMetrics(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(labService.getMetrics(currentUser));
    }

    @GetMapping("/critical-rules")
    @PreAuthorize("hasAnyRole('LAB_TECH', 'ADMIN')")
    public ResponseEntity<List<CriticalRuleResponse>> getCriticalRules(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(labService.getCriticalRules(currentUser));
    }

    @PostMapping("/critical-rules")
    @PreAuthorize("hasAnyRole('LAB_TECH', 'ADMIN')")
    public ResponseEntity<CriticalRuleResponse> createCriticalRule(
            @Valid @RequestBody CriticalRuleRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(labService.createCriticalRule(request, currentUser));
    }

    @PutMapping("/critical-rules/{id}/toggle")
    @PreAuthorize("hasAnyRole('LAB_TECH', 'ADMIN')")
    public ResponseEntity<CriticalRuleResponse> toggleCriticalRule(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(labService.toggleCriticalRule(id, currentUser));
    }

    @DeleteMapping("/critical-rules/{id}")
    @PreAuthorize("hasAnyRole('LAB_TECH', 'ADMIN')")
    public ResponseEntity<Void> deleteCriticalRule(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        labService.deleteCriticalRule(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/results/history")
    @PreAuthorize("hasAnyRole('LAB_TECH', 'DOCTOR', 'ADMIN')")
    public ResponseEntity<List<ResultHistoryResponse>> getResultHistory(
            @RequestParam Long orderId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(labService.getResultHistory(orderId, currentUser));
    }

    @GetMapping("/results/compare")
    @PreAuthorize("hasAnyRole('LAB_TECH', 'DOCTOR', 'ADMIN')")
    public ResponseEntity<List<ResultHistoryResponse>> compareResults(
            @RequestParam Long orderId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(labService.compareResults(orderId, currentUser));
    }

    @GetMapping("/trends/{patientId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'LAB_TECH', 'ADMIN')")
    public ResponseEntity<List<LabTrendResponse>> getPatientTrends(
            @PathVariable Long patientId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(labService.getPatientTrends(patientId, currentUser));
    }

    @GetMapping("/alerts")
    @PreAuthorize("hasAnyRole('LAB_TECH', 'DOCTOR', 'ADMIN')")
    public ResponseEntity<List<LabAlertResponse>> getLabAlerts(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(labService.getLabAlerts(currentUser));
    }

    @PostMapping("/alerts/{id}/acknowledge")
    @PreAuthorize("hasAnyRole('LAB_TECH', 'DOCTOR', 'ADMIN')")
    public ResponseEntity<LabAlertResponse> acknowledgeLabAlert(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(labService.acknowledgeLabAlert(id, currentUser));
    }

    @PostMapping("/results/import")
    @PreAuthorize("hasAnyRole('LAB_TECH', 'ADMIN')")
    public ResponseEntity<DeviceImportResponse> importDeviceResult(
            @Valid @RequestBody DeviceImportRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(labService.importDeviceResult(request, currentUser));
    }

    @PatchMapping("/sample/{id}/dispose")
    @PreAuthorize("hasAnyRole('LAB_TECH', 'ADMIN')")
    public ResponseEntity<SampleTrackingResponse> disposeSample(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(labService.disposeSample(id, currentUser));
    }

    @GetMapping("/sla-breaches")
    @PreAuthorize("hasAnyRole('ADMIN', 'LAB_TECH')")
    public ResponseEntity<List<SlaBreachResponse>> getSlaBreaches(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(labService.getSlaBreaches(currentUser));
    }
}

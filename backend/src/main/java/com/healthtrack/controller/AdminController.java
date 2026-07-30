package com.healthtrack.controller;

import com.healthtrack.dto.AdminDtos.*;
import com.healthtrack.entity.Role;
import com.healthtrack.security.UserPrincipal;
import com.healthtrack.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/dashboard/overview")
    public ResponseEntity<DashboardOverview> getDashboardOverview(@AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(adminService.getDashboardOverview(currentUser));
    }

    @GetMapping("/users/activity")
    public ResponseEntity<List<UserActivity>> getUserActivity(@AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(adminService.getUserActivity(currentUser));
    }

    @GetMapping("/users/login-history")
    public ResponseEntity<Page<LoginHistory>> getLoginHistory(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(adminService.getLoginHistory(currentUser.getHospitalId(), page, size));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deactivateUser(@PathVariable Long id) {
        adminService.deactivateUser(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<Void> updateUserRole(@PathVariable Long id, @RequestBody Role newRole) {
        adminService.updateUserRole(id, newRole);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/patients/{id}/full-history")
    public ResponseEntity<PatientFullHistory> getPatientFullHistory(
            @PathVariable Long id, @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(adminService.getPatientFullHistory(id, currentUser));
    }

    @GetMapping("/appointments/queue")
    public ResponseEntity<List<AppointmentQueueEntry>> getAppointmentQueue(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(adminService.getAppointmentQueue(currentUser.getHospitalId()));
    }

    @PostMapping("/appointments/bulk-cancel")
    public ResponseEntity<BulkCancelResponse> bulkCancelAppointments(@RequestBody BulkCancelRequest request) {
        return ResponseEntity.ok(adminService.bulkCancelAppointments(request));
    }

    @PutMapping("/appointments/{id}/priority")
    public ResponseEntity<Void> updateAppointmentPriority(
            @PathVariable Long id, @RequestBody PriorityUpdateRequest request) {
        adminService.updateAppointmentPriority(id, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/beds/status")
    public ResponseEntity<List<BedStatusResponse>> getBedStatus(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(adminService.getBedStatus(currentUser.getHospitalId()));
    }

    @PutMapping("/beds/transfer")
    public ResponseEntity<Void> transferBed(@RequestBody BedTransferRequest request) {
        adminService.transferBed(request.fromBedId(), request.toBedId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/revenue/summary")
    public ResponseEntity<RevenueSummary> getRevenueSummary(@AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(adminService.getRevenueSummary(currentUser.getHospitalId()));
    }

    @GetMapping("/revenue/trends")
    public ResponseEntity<List<RevenueTrend>> getRevenueTrends(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(defaultValue = "12") int months) {
        return ResponseEntity.ok(adminService.getRevenueTrends(currentUser.getHospitalId(), months));
    }

    @GetMapping("/bills/pending")
    public ResponseEntity<List<PendingBill>> getPendingBills(@AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(adminService.getPendingBills(currentUser.getHospitalId()));
    }

    @GetMapping("/lab/summary")
    public ResponseEntity<LabSummary> getLabSummary(@AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(adminService.getLabSummary(currentUser.getHospitalId()));
    }

    @GetMapping("/lab/abnormal")
    public ResponseEntity<List<LabAbnormal>> getAbnormalResults(@AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(adminService.getAbnormalResults(currentUser.getHospitalId()));
    }

    @GetMapping("/pharmacy/low-stock")
    public ResponseEntity<List<PharmacyLowStock>> getLowStock(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(adminService.getLowStockItems(currentUser.getHospitalId()));
    }

    @GetMapping("/pharmacy/expiring")
    public ResponseEntity<List<PharmacyExpiring>> getExpiringItems(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(adminService.getExpiringItems(currentUser.getHospitalId(), days));
    }

    @GetMapping("/pharmacy/sales")
    public ResponseEntity<PharmacySalesSummary> getPharmacySales(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(adminService.getPharmacySalesSummary(currentUser.getHospitalId()));
    }

    @GetMapping("/settings")
    public ResponseEntity<HospitalSettings> getSettings(@AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(adminService.getHospitalSettings(currentUser.getHospitalId()));
    }

    @PutMapping("/settings")
    public ResponseEntity<Void> updateSettings(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestBody HospitalSettingsUpdate update) {
        adminService.updateHospitalSettings(currentUser.getHospitalId(), update);
        return ResponseEntity.ok().build();
    }
}
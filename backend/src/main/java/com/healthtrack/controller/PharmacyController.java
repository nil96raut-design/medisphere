package com.healthtrack.controller;

import com.healthtrack.dto.PharmacyDtos.*;
import com.healthtrack.security.UserPrincipal;
import com.healthtrack.service.PharmacyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pharmacy")
@RequiredArgsConstructor
public class PharmacyController {

    private final PharmacyService pharmacyService;

    @GetMapping("/prescriptions/pending")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN', 'DOCTOR', 'NURSE')")
    public ResponseEntity<List<PendingPrescriptionResponse>> getPendingPrescriptions(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(pharmacyService.getPendingPrescriptions(currentUser));
    }

    @PostMapping("/dispense")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    public ResponseEntity<DispensationResponse> dispense(
            @Valid @RequestBody DispenseRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(pharmacyService.dispense(request, currentUser));
    }

    @GetMapping("/inventory")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN', 'DOCTOR', 'NURSE')")
    public ResponseEntity<List<MedicineStockResponse>> getInventory(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(pharmacyService.getAllStock(currentUser));
    }

    @GetMapping("/inventory/low-stock")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN', 'DOCTOR')")
    public ResponseEntity<List<MedicineStockResponse>> getLowStock(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(pharmacyService.getLowStock(currentUser));
    }

    @PostMapping("/inventory")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    public ResponseEntity<MedicineStockResponse> addStock(
            @Valid @RequestBody AddStockRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(pharmacyService.addStock(request, currentUser));
    }
}

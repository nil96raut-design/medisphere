package com.healthtrack.controller;

import com.healthtrack.dto.PharmacyDtos.*;
import com.healthtrack.entity.DispensationRecord;
import com.healthtrack.entity.ExpiryAlertType;
import com.healthtrack.security.UserPrincipal;
import com.healthtrack.service.ExpiryAlertService;
import com.healthtrack.service.PharmacyService;
import com.healthtrack.service.SupplierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
    private final SupplierService supplierService;
    private final ExpiryAlertService expiryAlertService;
    private final com.healthtrack.service.IdempotencyService idempotencyService;

    @GetMapping("/prescriptions/pending")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN', 'DOCTOR', 'NURSE')")
    public ResponseEntity<List<PendingPrescriptionItemResponse>> getPendingPrescriptions(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(pharmacyService.getPendingPrescriptions(currentUser));
    }

    @PostMapping("/dispense")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    public ResponseEntity<DispensationResponse> dispense(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody DispenseRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            if (!idempotencyService.tryProcess(idempotencyKey, currentUser.getHospitalId(), "DISPENSE")) {
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "Duplicate request");
            }
        }
        return ResponseEntity.ok(pharmacyService.dispense(request, currentUser));
    }

    @GetMapping("/inventory")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN', 'DOCTOR', 'NURSE')")
    public ResponseEntity<List<MedicineStockResponse>> getInventory(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(pharmacyService.getAllStock(currentUser));
    }

    @GetMapping("/inventory/medicine")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    public ResponseEntity<List<MedicineStockResponse>> getInventoryByMedicine(
            @RequestParam String name,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(pharmacyService.getInventoryByMedicine(currentUser, name));
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

    @PutMapping("/inventory/{stockId}/prices")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    public ResponseEntity<MedicineStockResponse> updateStockPrices(
            @PathVariable Long stockId,
            @Valid @RequestBody UpdateStockPriceRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(pharmacyService.updateStockPrices(stockId, request, currentUser));
    }

    @GetMapping("/dispensations")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    public ResponseEntity<Page<DispensationRecord>> getDispensationHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(pharmacyService.getDispensationHistory(
                currentUser.getHospitalId(), page, size));
    }

    @GetMapping("/dispensations/patient/{patientId}")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN', 'DOCTOR', 'NURSE')")
    public ResponseEntity<List<DispensationRecord>> getPatientDispensations(
            @PathVariable Long patientId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(pharmacyService.getPatientDispensations(patientId));
    }

    @GetMapping("/suppliers")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    public ResponseEntity<List<SupplierResponse>> getSuppliers(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(supplierService.getSuppliers(currentUser));
    }

    @PostMapping("/suppliers")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    public ResponseEntity<SupplierResponse> createSupplier(
            @Valid @RequestBody CreateSupplierRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(supplierService.createSupplier(request, currentUser));
    }

    @PutMapping("/suppliers/{id}")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    public ResponseEntity<SupplierResponse> updateSupplier(
            @PathVariable Long id,
            @Valid @RequestBody CreateSupplierRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(supplierService.updateSupplier(id, request, currentUser));
    }

    @PostMapping("/suppliers/{id}/toggle")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    public ResponseEntity<Void> toggleSupplier(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        supplierService.toggleSupplierStatus(id, currentUser);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/purchase-orders")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    public ResponseEntity<List<PurchaseOrderResponse>> getPurchaseOrders(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(supplierService.getPurchaseOrders(currentUser));
    }

    @PostMapping("/purchase-orders")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    public ResponseEntity<PurchaseOrderResponse> createPurchaseOrder(
            @Valid @RequestBody CreatePurchaseOrderRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(supplierService.createPurchaseOrder(request, currentUser));
    }

    @PostMapping("/purchase-orders/{orderId}/receive")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    public ResponseEntity<PurchaseOrderResponse> receivePurchaseOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(supplierService.receivePurchaseOrder(orderId, currentUser));
    }

    @PostMapping("/purchase-orders/{orderId}/cancel")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    public ResponseEntity<PurchaseOrderResponse> cancelPurchaseOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(supplierService.cancelPurchaseOrder(orderId, currentUser));
    }

    @GetMapping("/reorder-suggestions")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    public ResponseEntity<List<ReorderSuggestionResponse>> getReorderSuggestions(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(supplierService.getReorderSuggestions(currentUser));
    }

    @GetMapping("/expiry-alerts")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN', 'DOCTOR')")
    public ResponseEntity<List<com.healthtrack.dto.PharmacyDtos.ExpiryAlertResponse>> getExpiryAlerts(
            @RequestParam(required = false) String type,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        if (type != null) {
            return ResponseEntity.ok(expiryAlertService
                    .getAlertsByType(currentUser, ExpiryAlertType.valueOf(type)));
        }
        return ResponseEntity.ok(expiryAlertService.getActiveAlerts(currentUser));
    }

    @PostMapping("/expiry-alerts/{alertId}/resolve")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    public ResponseEntity<Void> resolveExpiryAlert(
            @PathVariable Long alertId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        expiryAlertService.resolveAlert(alertId, currentUser);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/inventory/recall")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    public ResponseEntity<PharmacyRecallResponse> flagRecall(
            @Valid @RequestBody RecallBatchRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(pharmacyService.flagRecall(request, currentUser));
    }
}

package com.healthtrack.controller;

import com.healthtrack.dto.BillingDtos.*;
import com.healthtrack.security.UserPrincipal;
import com.healthtrack.service.BillingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;

    @GetMapping("/calculate/{patientId}")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'ADMIN')")
    public ResponseEntity<CalculateResponse> calculate(
            @PathVariable Long patientId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(billingService.calculate(patientId, currentUser));
    }

    @PostMapping("/settle")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'ADMIN')")
    public ResponseEntity<BillResponse> settle(
            @Valid @RequestBody SettleRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(billingService.settle(request, currentUser));
    }

    @PostMapping("/{id}/refund")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<RefundResponse> refund(
            @PathVariable Long id,
            @Valid @RequestBody RefundRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(billingService.refund(id, request, currentUser));
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'ADMIN')")
    public ResponseEntity<BillResponse> pay(
            @PathVariable Long id,
            @Valid @RequestBody PaymentRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(billingService.pay(id, request, currentUser));
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'ADMIN')")
    public ResponseEntity<List<BillResponse>> getPatientBills(
            @PathVariable Long patientId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(billingService.getPatientBills(patientId, currentUser));
    }
}

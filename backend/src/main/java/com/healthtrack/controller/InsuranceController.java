package com.healthtrack.controller;

import com.healthtrack.entity.InsuranceClaim;
import com.healthtrack.service.InsuranceClaimService;
import com.healthtrack.security.UserPrincipal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/admin/insurance")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class InsuranceController {

    private final InsuranceClaimService insuranceClaimService;

    @PostMapping("/claims/{billId}/create")
    public ResponseEntity<InsuranceClaim> createClaim(@PathVariable Long billId,
                                                       @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(insuranceClaimService.createClaim(billId, currentUser));
    }

    @PostMapping("/claims/{claimId}/submit")
    public ResponseEntity<InsuranceClaim> submitClaim(@PathVariable Long claimId,
                                                       @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(insuranceClaimService.submitClaim(claimId, currentUser));
    }

    @PostMapping("/claims/{claimId}/approve")
    public ResponseEntity<InsuranceClaim> approveClaim(@PathVariable Long claimId,
                                                        @RequestParam @NotNull BigDecimal approvedAmount,
                                                        @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(insuranceClaimService.approveClaim(claimId, approvedAmount, currentUser));
    }

    @PostMapping("/claims/{claimId}/reject")
    public ResponseEntity<InsuranceClaim> rejectClaim(@PathVariable Long claimId,
                                                       @RequestParam @NotBlank String reason,
                                                       @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(insuranceClaimService.rejectClaim(claimId, reason, currentUser));
    }

    @GetMapping("/claims")
    public ResponseEntity<Page<InsuranceClaim>> getClaims(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(insuranceClaimService.getClaims(status, page, size, currentUser));
    }
}

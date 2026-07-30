package com.healthtrack.service;

import com.healthtrack.entity.*;
import com.healthtrack.event.EventConstants;
import com.healthtrack.event.EventPublisher;
import com.healthtrack.repository.*;
import com.healthtrack.security.TenantValidator;
import com.healthtrack.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InsuranceClaimService {

    private final InsuranceClaimRepository insuranceClaimRepository;
    private final BillRepository billRepository;
    private final PatientRepository patientRepository;
    private final TenantValidator tenantValidator;
    private final EventPublisher eventPublisher;

    @Transactional
    public InsuranceClaim createClaim(Long billId, UserPrincipal currentUser) {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bill not found"));
        tenantValidator.validateHospitalAccess(bill.getHospital().getId(), currentUser.getHospitalId());

        if (bill.getInsuranceCoveredAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No insurance coverage on this bill");
        }

        Patient patient = bill.getPatient();
        if (patient.getInsuranceProvider() == null || patient.getPolicyNumber() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Patient has no insurance info on file");
        }

        InsuranceClaim claim = InsuranceClaim.builder()
                .bill(bill)
                .hospital(bill.getHospital())
                .patient(patient)
                .claimAmount(bill.getInsuranceCoveredAmount())
                .insurerName(patient.getInsuranceProvider())
                .policyNumber(patient.getPolicyNumber())
                .status("INITIATED")
                .build();

        claim = insuranceClaimRepository.save(claim);

        Map<String, Object> payload = new HashMap<>();
        payload.put("claimId", claim.getId());
        payload.put("billId", bill.getId());
        payload.put("patientId", patient.getId());
        payload.put("amount", bill.getInsuranceCoveredAmount());
        eventPublisher.publish(EventConstants.INSURANCE_CLAIM_SUBMITTED, currentUser.getHospitalId(), payload);

        return claim;
    }

    @Transactional
    public InsuranceClaim submitClaim(Long claimId, UserPrincipal currentUser) {
        InsuranceClaim claim = findAndValidate(claimId, currentUser);
        if (!"INITIATED".equals(claim.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Claim already " + claim.getStatus());
        }
        claim.setStatus("SUBMITTED");
        claim.setSubmittedAt(OffsetDateTime.now());
        return insuranceClaimRepository.save(claim);
    }

    @Transactional
    public InsuranceClaim approveClaim(Long claimId, BigDecimal approvedAmount, UserPrincipal currentUser) {
        InsuranceClaim claim = findAndValidate(claimId, currentUser);
        if (!"SUBMITTED".equals(claim.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only SUBMITTED claims can be approved");
        }
        claim.setStatus("APPROVED");
        claim.setApprovedAmount(approvedAmount);
        claim.setApprovedAt(OffsetDateTime.now());
        return insuranceClaimRepository.save(claim);
    }

    @Transactional
    public InsuranceClaim rejectClaim(Long claimId, String reason, UserPrincipal currentUser) {
        InsuranceClaim claim = findAndValidate(claimId, currentUser);
        if (!"SUBMITTED".equals(claim.getStatus()) && !"INITIATED".equals(claim.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only INITIATED or SUBMITTED claims can be rejected");
        }
        claim.setStatus("REJECTED");
        claim.setRejectionReason(reason);
        claim.setRejectedAt(OffsetDateTime.now());
        return insuranceClaimRepository.save(claim);
    }

    @Transactional(readOnly = true)
    public Page<InsuranceClaim> getClaims(String status, int page, int size, UserPrincipal currentUser) {
        PageRequest pr = PageRequest.of(page, Math.min(size, 100));
        if (status != null && !status.isBlank()) {
            return insuranceClaimRepository.findByHospitalIdAndStatus(currentUser.getHospitalId(), status, pr);
        }
        return insuranceClaimRepository.findByHospitalId(currentUser.getHospitalId(), pr);
    }

    private InsuranceClaim findAndValidate(Long claimId, UserPrincipal currentUser) {
        InsuranceClaim claim = insuranceClaimRepository.findById(claimId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Claim not found"));
        tenantValidator.validateHospitalAccess(claim.getHospital().getId(), currentUser.getHospitalId());
        return claim;
    }
}

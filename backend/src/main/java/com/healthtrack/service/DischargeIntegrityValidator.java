package com.healthtrack.service;

import com.healthtrack.entity.*;
import com.healthtrack.repository.DispensationRecordRepository;
import com.healthtrack.repository.LabTestOrderRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DischargeIntegrityValidator {

    private static final Logger log = LoggerFactory.getLogger(DischargeIntegrityValidator.class);

    private final LabTestOrderRepository labTestOrderRepository;
    private final DispensationRecordRepository dispensationRecordRepository;

    public void validateDischarge(Admission admission) {
        Long patientId = admission.getPatient().getId();

        // 1. Check pending lab test orders
        List<LabTestOrder> labs = labTestOrderRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
        boolean hasPendingLabs = labs.stream().anyMatch(l ->
                l.getStatus() == LabOrderStatus.ORDERED ||
                l.getStatus() == LabOrderStatus.SAMPLE_COLLECTED ||
                l.getStatus() == LabOrderStatus.PROCESSING ||
                l.getStatus() == LabOrderStatus.RESULT_ENTERED);

        if (hasPendingLabs) {
            log.warn("Discharge blocked for patient ID {}: Pending lab orders exist", patientId);
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot discharge patient: There are pending unapproved lab orders.");
        }

        // 2. Check unpaid pharmacy dispensations
        boolean hasPendingDispensations = dispensationRecordRepository.existsByPatientIdAndBillingStatus(patientId, BillingStatus.PENDING);
        if (hasPendingDispensations) {
            log.warn("Discharge blocked for patient ID {}: Pending unpaid pharmacy dispensations exist", patientId);
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot discharge patient: Unsettled pharmacy dispensations exist.");
        }

        log.info("Discharge integrity validation passed for admission ID {}, patient ID {}", admission.getId(), patientId);
    }
}

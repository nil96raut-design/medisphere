package com.healthtrack.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class IpdDtos {

    public record BedResponse(
            Long id,
            String wardName,
            String bedNumber,
            BigDecimal chargePerDay,
            Boolean isOccupied
    ) {}

    public record AdmissionRequest(
            @NotNull Long patientId,
            @NotNull Long doctorId,
            @NotNull Long bedId,
            @NotNull LocalDate admissionDate,
            String initialDiagnosis
    ) {}

    public record AdmissionResponse(
            Long id,
            Long patientId,
            String patientName,
            Long doctorId,
            String doctorName,
            Long bedId,
            String wardName,
            String bedNumber,
            LocalDate admissionDate,
            LocalDate dischargeDate,
            String initialDiagnosis,
            String dischargeSummary,
            String status
    ) {}

    public record NursingLogRequest(
            String vitalsRecorded,
            String medicineAdministered,
            String nursingNotes
    ) {}

    public record NursingLogResponse(
            Long id,
            Long admissionId,
            Long nurseId,
            String nurseName,
            String vitalsRecorded,
            String medicineAdministered,
            String nursingNotes,
            LocalDateTime loggedAt
    ) {}

    public record DischargeRequest(
            String dischargeSummary
    ) {}
}

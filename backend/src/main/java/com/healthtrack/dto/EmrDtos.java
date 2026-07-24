package com.healthtrack.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public class EmrDtos {

    public record PrescriptionItemRequest(
            @NotBlank String medicineName,
            @NotBlank String dosage,
            @NotBlank String frequency,
            @NotBlank String duration,
            String instructions
    ) {}

    public record ServiceRequestEntry(
            @NotBlank String serviceType,
            String serviceDetails
    ) {}

    public record CreateMedicalRecordRequest(
            @NotNull Long patientId,
            Long appointmentId,
            @NotNull LocalDate encounterDate,
            @NotBlank String chiefComplaints,
            String objectiveFindings,
            String diagnosis,
            LocalDate nextFollowUpDate,
            @Valid List<PrescriptionItemRequest> prescriptions,
            @Valid List<ServiceRequestEntry> serviceRequests
    ) {}

    public record PrescriptionItemResponse(
            Long id,
            String medicineName,
            String dosage,
            String frequency,
            String duration,
            String instructions
    ) {}

    public record ServiceRequestResponse(
            Long id,
            String serviceType,
            String serviceDetails,
            String status
    ) {}

    public record MedicalRecordResponse(
            Long id,
            Long patientId,
            String patientName,
            Long doctorId,
            String doctorName,
            LocalDate encounterDate,
            String chiefComplaints,
            String objectiveFindings,
            String diagnosis,
            LocalDate nextFollowUpDate,
            List<PrescriptionItemResponse> prescriptions,
            List<ServiceRequestResponse> serviceRequests
    ) {}
}

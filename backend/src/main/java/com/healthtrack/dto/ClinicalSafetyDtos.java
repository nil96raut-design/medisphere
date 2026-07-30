package com.healthtrack.dto;

import com.healthtrack.entity.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.List;

public class ClinicalSafetyDtos {

    public record MedicationScheduleResponse(
            Long id,
            Long prescriptionItemId,
            Long patientId,
            Long nurseId,
            OffsetDateTime scheduledTime,
            MedicationScheduleStatus status,
            OffsetDateTime createdAt
    ) {}

    public record AlertResponse(
            Long id,
            Long patientId,
            String patientName,
            AlertType type,
            AlertSeverity severity,
            AlertStatus status,
            String message,
            OffsetDateTime createdAt,
            Long acknowledgedBy,
            String acknowledgedByName,
            OffsetDateTime acknowledgedAt,
            Long escalatedTo,
            String escalatedToName,
            OffsetDateTime escalatedAt,
            Long resolvedBy,
            OffsetDateTime resolvedAt
    ) {}

    public record ShiftHandoverRequest(
            @NotNull Long toNurseId,
            String wardName,
            String notes,
            String patientSummary
    ) {}

    public record ShiftHandoverResponse(
            Long id,
            Long fromNurseId,
            String fromNurseName,
            Long toNurseId,
            String toNurseName,
            String wardName,
            String notes,
            String patientSummary,
            OffsetDateTime createdAt
    ) {}

    public record BedCleaningResponse(
            Long id,
            Long bedId,
            String bedNumber,
            String wardName,
            Long requestedById,
            String requestedByName,
            CleaningStatus status,
            OffsetDateTime createdAt,
            OffsetDateTime cleanedAt
    ) {}
}

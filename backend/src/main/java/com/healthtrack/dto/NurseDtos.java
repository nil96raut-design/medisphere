package com.healthtrack.dto;

import com.healthtrack.entity.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public class NurseDtos {

    public record AssignNurseRequest(
            @NotNull Long nurseId,
            @NotNull Long patientId,
            Long bedId
    ) {}

    public record NurseAssignmentResponse(
            Long id,
            Long nurseId,
            String nurseName,
            Long patientId,
            String patientName,
            Long bedId,
            String bedNumber,
            String wardName,
            NurseAssignmentStatus status,
            OffsetDateTime assignedAt,
            OffsetDateTime releasedAt
    ) {}

    public record VitalRecordRequest(
            @NotNull Long patientId,
            String bloodPressure,
            Integer heartRate,
            BigDecimal temperature,
            Integer spo2,
            BigDecimal sugarLevel
    ) {}

    public record VitalRecordResponse(
            Long id,
            Long patientId,
            String patientName,
            Long nurseId,
            String nurseName,
            String bloodPressure,
            Integer heartRate,
            BigDecimal temperature,
            Integer spo2,
            BigDecimal sugarLevel,
            Boolean alertFlag,
            String alertReason,
            OffsetDateTime recordedAt
    ) {}

    public record MedicationAdminRequest(
            @NotNull Long prescriptionItemId,
            @NotNull Long patientId,
            String notes
    ) {}

    public record MedicationAdminResponse(
            Long id,
            Long prescriptionItemId,
            Long patientId,
            String patientName,
            Long nurseId,
            String nurseName,
            String medicineName,
            MedicationStatus status,
            OffsetDateTime administeredAt,
            String notes
    ) {}

    public record NursingNoteRequest(
            @NotNull Long patientId,
            @NotBlank String note
    ) {}

    public record NursingNoteResponse(
            Long id,
            Long patientId,
            String patientName,
            Long nurseId,
            String nurseName,
            String note,
            OffsetDateTime createdAt
    ) {}

    public record NurseTaskRequest(
            @NotNull Long nurseId,
            @NotNull Long patientId,
            @NotNull NurseTaskType taskType,
            OffsetDateTime dueTime,
            Boolean isRecurring,
            Integer recurrenceIntervalMinutes,
            TaskPriority priority,
            String source
    ) {}

    public record NurseTaskResponse(
            Long id,
            Long nurseId,
            String nurseName,
            Long patientId,
            String patientName,
            NurseTaskType taskType,
            NurseTaskStatus status,
            OffsetDateTime dueTime,
            OffsetDateTime completedAt,
            OffsetDateTime createdAt,
            Boolean isRecurring,
            Integer recurrenceIntervalMinutes,
            TaskPriority priority,
            String source
    ) {}

    public record AssignedPatientResponse(
            Long patientId,
            String patientName,
            Long bedId,
            String bedNumber,
            String wardName,
            OffsetDateTime assignedAt,
            List<NurseTaskResponse> pendingTasks
    ) {}

    public record VitalTrendResponse(
            Long patientId,
            String patientName,
            List<VitalRecordResponse> recentVitals,
            Boolean consecutiveAbnormal,
            int consecutiveAbnormalCount,
            String trend
    ) {}
}

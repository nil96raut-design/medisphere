package com.healthtrack.dto;

import com.healthtrack.entity.WalkInQueueStatus;
import com.healthtrack.entity.QueuePriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class FrontDeskDtos {

    public record WalkInRequest(
            @NotNull Long patientId,
            @NotNull Long doctorId,
            QueuePriority priority,
            String notes
    ) {}

    public record WalkInResponse(
            Long id,
            Long patientId,
            String patientName,
            Long doctorId,
            String doctorName,
            Integer tokenNo,
            WalkInQueueStatus status,
            QueuePriority priority,
            String notes,
            LocalDateTime createdAt
    ) {}

    public record QueueOrderRequest(
            @NotNull Long doctorId
    ) {}

    public record QueueEntry(
            Long id,
            String source,
            Integer tokenNo,
            Long patientId,
            String patientName,
            WalkInQueueStatus status,
            QueuePriority priority,
            LocalDateTime createdAt,
            String notes
    ) {}

    public record QueueStatusUpdateRequest(
            @NotNull WalkInQueueStatus status,
            String notes
    ) {}

    public record DuplicateCheckRequest(
            @NotBlank String phoneNumber,
            String firstName,
            String lastName
    ) {}

    public record DuplicateCheckResponse(
            boolean duplicateExists,
            List<MatchResult> matches
    ) {
        public record MatchResult(
                Long patientId,
                String fullName,
                String phoneNumber,
                String email,
                double score
        ) {}
    }

    public record ProvisionalAdmissionRequest(
            @NotNull Long patientId,
            @NotNull Long bedId,
            @NotNull Long doctorId,
            @NotBlank String initialDiagnosis,
            LocalDate admissionDate
    ) {}

    public record BillingInitiateRequest(
            @NotNull Long patientId,
            List<LineItem> items
    ) {
        public record LineItem(
                String description,
                @NotNull BigDecimal amount,
                Integer quantity
            ) {}
    }

    public record DailySummaryResponse(
            long totalWalkIns,
            long totalAppointments,
            long newPatients,
            long emergencyCases,
            long pendingQueue,
            long totalAdmissions,
            long totalBillsInitiated,
            List<DoctorQueueSummary> doctorQueues
    ) {
        public record DoctorQueueSummary(
                Long doctorId,
                String doctorName,
                int queueLength,
                int emergencyCount
        ) {}
    }
}

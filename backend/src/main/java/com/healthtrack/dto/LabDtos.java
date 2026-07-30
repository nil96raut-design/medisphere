package com.healthtrack.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class LabDtos {

    public record LabOrderResponse(
            Long id,
            Long patientId,
            String patientName,
            String testName,
            String requestedByName,
            String status,
            String resultValues,
            String technicianNotes,
            LocalDateTime completedAt,
            LocalDateTime createdAt,
            LocalDateTime sampleCollectedAt,
            LocalDateTime processingStartedAt,
            LocalDateTime processingCompletedAt,
            String sampleBarcode,
            String sampleStorageLocation,
            Boolean criticalFlag,
            Integer turnaroundMinutes,
            Long retestOfId,
            String correctionReason,
            String resultEnteredByName,
            BigDecimal price,
            List<SampleTrackingResponse> sampleTrackings
    ) {}

    public record LabOrderListResponse(
            Long id,
            Long patientId,
            String patientName,
            String testName,
            String requestedByName,
            String status,
            LocalDateTime createdAt,
            LocalDateTime sampleCollectedAt,
            Boolean criticalFlag,
            Integer turnaroundMinutes,
            String sampleBarcode
    ) {}

    public record SampleCollectionRequest(
            @NotBlank String technicianNotes,
            String sampleType,
            String containerType,
            String barcode,
            String collectionVolume,
            String collectionMethod,
            String storageLocation,
            String storageCondition
    ) {}

    public record ProcessRequest(
            String technicianNotes
    ) {}

    public record ResultEntryRequest(
            @NotBlank String resultValues,
            String technicianNotes,
            String structuredResults
    ) {}

    public record StructuredResult(
            String parameterName,
            String value,
            String unit,
            String referenceRange,
            Boolean isAbnormal,
            Boolean isCritical
    ) {}

    public record RetestRequest(
            String correctionReason
    ) {}

    public record ApproveRequest(
            String notes
    ) {}

    public record CriticalRuleRequest(
            @NotBlank String testName,
            @NotBlank String parameterName,
            @NotBlank String conditionOperator,
            @NotBlank String thresholdValue,
            String unit,
            String severity
    ) {}

    public record CriticalRuleResponse(
            Long id,
            String testName,
            String parameterName,
            String conditionOperator,
            String thresholdValue,
            String unit,
            String severity,
            Boolean enabled
    ) {}

    public record SampleTrackingResponse(
            Long id,
            Long labOrderId,
            String sampleType,
            String containerType,
            String barcode,
            String collectionVolume,
            String collectionMethod,
            String storageLocation,
            String storageCondition,
            String collectedByName,
            LocalDateTime collectedAt,
            String notes
    ) {}

    public record LabMetricsResponse(
            long pendingCollection,
            long inProcessing,
            long pendingApproval,
            long completedToday,
            long criticalResults,
            long retests,
            Double avgTurnaroundMinutes,
            long totalOrdersToday
    ) {}

    public record LabTechQueueResponse(
            List<LabOrderListResponse> pendingCollection,
            List<LabOrderListResponse> inProcessing,
            List<LabOrderListResponse> pendingResults,
            List<LabOrderListResponse> criticalResults,
            LabMetricsResponse metrics
    ) {}

    public record ResultHistoryResponse(
            Long id,
            Long labOrderId,
            Integer version,
            String resultData,
            String createdByName,
            Boolean isActive,
            LocalDateTime createdAt
    ) {}

    public record TrendDataPoint(
            String date,
            String value,
            String unit,
            String referenceRange
    ) {}

    public record LabTrendResponse(
            String testName,
            String parameterName,
            List<TrendDataPoint> dataPoints
    ) {}

    public record DeviceImportRequest(
            @NotBlank String sampleId,
            @NotBlank String testName,
            @NotNull Object values
    ) {}

    public record DeviceImportResponse(
            Long labOrderId,
            Integer version,
            Boolean criticalFlag
    ) {}

    public record SlaBreachResponse(
            Long id,
            Long labOrderId,
            String patientName,
            String testName,
            Integer expectedTatMinutes,
            Integer actualTatMinutes,
            Boolean notified,
            LocalDateTime breachedAt
    ) {}

    public record LabAlertResponse(
            Long id,
            Long patientId,
            String patientName,
            String severity,
            String status,
            String message,
            Long labOrderId,
            String acknowledgedByName,
            LocalDateTime createdAt
    ) {}
}

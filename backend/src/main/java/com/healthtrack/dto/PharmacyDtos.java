package com.healthtrack.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class PharmacyDtos {

    public record MedicineStockResponse(
            Long id,
            String medicineName,
            String batchNumber,
            LocalDate expiryDate,
            Integer availableQuantity,
            Integer quantityReserved,
            Integer effectiveQuantity,
            Integer reorderLevel,
            BigDecimal unitPrice,
            BigDecimal purchasePrice,
            BigDecimal sellingPrice,
            Long supplierId,
            String supplierName,
            boolean isExpired,
            boolean isLowStock
    ) {}

    public record AddStockRequest(
            @NotBlank String medicineName,
            @NotBlank String batchNumber,
            @NotNull LocalDate expiryDate,
            @Min(1) Integer quantity,
            BigDecimal unitPrice,
            BigDecimal purchasePrice,
            BigDecimal sellingPrice,
            Long supplierId
    ) {}

    public record UpdateStockPriceRequest(
            BigDecimal unitPrice,
            BigDecimal sellingPrice,
            BigDecimal purchasePrice,
            Integer reorderLevel
    ) {}

    public record DispenseRequest(
            @NotNull Long patientId,
            @NotNull String medicineName,
            @Min(1) Integer quantity,
            Long prescriptionItemId
    ) {}

    public record DispenseBatchResponse(
            Long stockId,
            String batchNumber,
            LocalDate expiryDate,
            Integer quantityTaken,
            BigDecimal unitPrice,
            BigDecimal subtotal
    ) {}

    public record DispensationResponse(
            Long id,
            String medicineName,
            Integer quantityDispensed,
            Integer remainingQuantity,
            String dispensationStatus,
            BigDecimal totalPrice,
            String billingStatus,
            LocalDateTime dispensedAt,
            String dispensedByName,
            java.util.List<DispenseBatchResponse> batches
    ) {}

    public record PendingPrescriptionItemResponse(
            Long prescriptionItemId,
            String medicineName,
            String dosage,
            String frequency,
            String duration,
            String instructions,
            String patientName,
            Long patientId,
            String doctorName,
            Integer alreadyDispensed,
            Integer prescribedQuantity
    ) {}

    public record SupplierResponse(
            Long id,
            String name,
            String contactPerson,
            String contactNumber,
            String email,
            String address,
            String gstNumber,
            boolean isActive
    ) {}

    public record CreateSupplierRequest(
            @NotBlank String name,
            String contactPerson,
            String contactNumber,
            String email,
            String address,
            String gstNumber
    ) {}

    public record PurchaseOrderResponse(
            Long id,
            Long supplierId,
            String supplierName,
            String medicineName,
            Integer quantityOrdered,
            Integer quantityReceived,
            BigDecimal unitPrice,
            BigDecimal totalPrice,
            String status,
            String notes,
            String orderedAt,
            String receivedAt
    ) {}

    public record CreatePurchaseOrderRequest(
            Long supplierId,
            @NotBlank String medicineName,
            @Min(1) Integer quantityOrdered,
            BigDecimal unitPrice,
            BigDecimal totalPrice,
            String notes
    ) {}

    public record ExpiryAlertResponse(
            Long id,
            Long medicineStockId,
            String medicineName,
            String batchNumber,
            LocalDate expiryDate,
            String alertType,
            boolean isResolved
    ) {}

    public record ReorderSuggestionResponse(
            String medicineName,
            Integer currentStock,
            Integer reorderLevel,
            Integer suggestedQuantity,
            String reason
    ) {}

    public record DispensationHistoryResponse(
            Long id,
            String medicineName,
            Integer quantity,
            BigDecimal totalPrice,
            String patientName,
            LocalDateTime dispensedAt,
            String dispensedByName,
            String status
    ) {}

    public record RecallBatchRequest(
            @NotBlank String medicineName,
            @NotBlank String batchNumber,
            @NotBlank String recallReason
    ) {}

    public record PharmacyRecallResponse(
            Long id,
            String medicineName,
            String batchNumber,
            String recallReason,
            boolean active,
            java.time.OffsetDateTime recalledAt
    ) {}
}

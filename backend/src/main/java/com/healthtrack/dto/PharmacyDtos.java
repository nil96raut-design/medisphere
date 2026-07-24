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
            Integer reorderLevel,
            BigDecimal unitPrice,
            boolean isExpired,
            boolean isLowStock
    ) {}

    public record AddStockRequest(
            @NotBlank String medicineName,
            @NotBlank String batchNumber,
            @NotNull LocalDate expiryDate,
            @Min(1) Integer quantity,
            BigDecimal unitPrice
    ) {}

    public record DispenseRequest(
            @NotNull Long patientId,
            @NotNull Long medicineStockId,
            @Min(1) Integer quantity,
            Long prescriptionItemId
    ) {}

    public record DispensationResponse(
            Long id,
            String medicineName,
            Integer quantityDispensed,
            BigDecimal totalPrice,
            String billingStatus,
            LocalDateTime dispensedAt,
            String dispensedByName
    ) {}

    public record PendingPrescriptionResponse(
            Long prescriptionItemId,
            String medicineName,
            String dosage,
            String frequency,
            String duration,
            String patientName,
            String doctorName
    ) {}
}

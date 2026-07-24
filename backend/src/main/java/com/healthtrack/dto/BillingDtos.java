package com.healthtrack.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public class BillingDtos {

    public record LineItem(
            String description,
            BigDecimal amount,
            Integer quantity,
            BigDecimal total
    ) {}

    public record CalculateResponse(
            Long patientId,
            String patientName,
            List<LineItem> items,
            BigDecimal totalAmount,
            BigDecimal totalDiscount,
            BigDecimal insuranceCovered,
            BigDecimal netPayable
    ) {}

    public record SettleRequest(
            @NotNull Long patientId,
            BigDecimal discountAmount,
            BigDecimal insuranceCoveredAmount,
            @NotNull String paymentMode,
            String idempotencyKey
    ) {}

    public record BillResponse(
            Long id,
            Long patientId,
            String patientName,
            BigDecimal totalAmount,
            BigDecimal discountAmount,
            BigDecimal insuranceCoveredAmount,
            BigDecimal netPayable,
            String paymentStatus,
            String paymentMode,
            OffsetDateTime createdAt
    ) {}
}

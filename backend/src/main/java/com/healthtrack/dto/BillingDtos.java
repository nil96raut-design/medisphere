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
            BigDecimal total,
            String sourceType,
            Long sourceId
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
            BigDecimal amountPaid,
            @NotNull String paymentMode,
            String transactionReference,
            String idempotencyKey
    ) {}

    public record PaymentRequest(
            @NotNull BigDecimal amount,
            @NotNull String paymentMode,
            @NotNull String transactionReference,
            String remarks
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
            OffsetDateTime createdAt,
            String refundReason,
            OffsetDateTime refundedAt,
            BigDecimal refundedAmount
    ) {}

    public record RefundRequest(
            @NotNull String reason,
            BigDecimal amount
    ) {}

    public record RefundResponse(
            Long id,
            String paymentStatus,
            String refundReason,
            OffsetDateTime refundedAt,
            BigDecimal refundedAmount
    ) {}
}

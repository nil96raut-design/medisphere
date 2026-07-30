package com.healthtrack.dto;

import com.healthtrack.entity.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

public class AdminDtos {

    public record DashboardOverview(
            long totalPatientsToday,
            long totalPatientsMonth,
            long appointmentsToday,
            BigDecimal revenueToday,
            BigDecimal revenueMonth,
            double bedOccupancyPercentage,
            long labTestsCompletedToday,
            long pendingBills
    ) {}

    public record UserActivity(
            Long id,
            String fullName,
            String email,
            String role,
            boolean active,
            OffsetDateTime lastActive,
            long actionCount
    ) {}

    public record LoginHistory(
            Long userId,
            String fullName,
            String action,
            String details,
            OffsetDateTime timestamp
    ) {}

    public record UserStatusUpdate(String status) {}

    public record PatientFullHistory(
            PatientDtos.PatientResponse patient,
            List<AppointmentDtos.AppointmentResponse> appointments,
            List<BillingDtos.BillResponse> bills,
            List<LabDtos.LabOrderResponse> labOrders,
            List<EmrDtos.MedicalRecordResponse> prescriptions
    ) {}

    public record AppointmentQueueEntry(
            Long id,
            Long patientId,
            String patientName,
            Long doctorId,
            String doctorName,
            LocalDate date,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String status,
            Integer tokenNumber,
            int priority
    ) {}

    public record BulkCancelRequest(List<Long> appointmentIds, String reason) {}

    public record BulkCancelResponse(int cancelled, int failed) {}

    public record PriorityUpdateRequest(int priority) {}

    public record BedStatusResponse(
            Long id,
            String bedNumber,
            String wardName,
            boolean occupied,
            BigDecimal chargePerDay,
            Long currentPatientId,
            String currentPatientName
    ) {}

    public record BedTransferRequest(
            Long fromBedId,
            Long toBedId
    ) {}

    public record RevenueSummary(
            BigDecimal totalRevenue,
            BigDecimal monthlyRevenue,
            BigDecimal pendingAmount,
            long paidBills,
            long pendingBills,
            long refundedBills
    ) {}

    public record RevenueTrend(
            String month,
            BigDecimal revenue,
            long count
    ) {}

    public record PendingBill(
            Long id,
            Long patientId,
            String patientName,
            BigDecimal amount,
            OffsetDateTime createdAt,
            String patientPhone
    ) {}

    public record LabSummary(
            long ordered,
            long sampleCollected,
            long resultReady,
            long pendingApproval,
            long approved,
            long abnormal
    ) {}

    public record LabAbnormal(
            Long id,
            Long patientId,
            String patientName,
            String testName,
            String resultValues,
            String status
    ) {}

    public record PharmacyLowStock(
            Long id,
            String medicineName,
            String batchNumber,
            int availableQuantity,
            int reorderLevel,
            LocalDate expiryDate
    ) {}

    public record PharmacyExpiring(
            Long id,
            String medicineName,
            String batchNumber,
            int availableQuantity,
            LocalDate expiryDate,
            int daysUntilExpiry
    ) {}

    public record PharmacySalesSummary(
            long totalDispensed,
            long lowStockItems,
            long expiringItems,
            BigDecimal monthlySales
    ) {}

    public record HospitalSettings(
            String hospitalName,
            String contactEmail,
            String subscriptionTier,
            String subscriptionStatus,
            OffsetDateTime trialEndDate,
            BigDecimal defaultConsultationFee,
            Integer lowStockThreshold
    ) {}

    public record HospitalSettingsUpdate(
            String hospitalName,
            String contactEmail,
            BigDecimal defaultConsultationFee,
            Integer lowStockThreshold
    ) {}
}
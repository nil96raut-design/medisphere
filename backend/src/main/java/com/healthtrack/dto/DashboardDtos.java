package com.healthtrack.dto;

import java.math.BigDecimal;
import java.util.List;

public class DashboardDtos {

    public record AnalyticsResponse(
            long totalPatients,
            long todayAppointments,
            BigDecimal todayRevenue,
            long activeBeds,
            long totalBeds,
            long pendingLabOrders,
            long lowStockItems,
            long activeAdmissions,
            List<RevenueTrend> weeklyRevenue,
            List<AppointmentTrend> todaySchedule,
            boolean degraded,
            java.time.OffsetDateTime lastUpdated
    ) {}

    public record RevenueTrend(
            String date,
            BigDecimal amount
    ) {}

    public record AppointmentTrend(
            String hour,
            long count
    ) {}
}

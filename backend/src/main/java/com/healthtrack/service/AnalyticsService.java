package com.healthtrack.service;

import com.healthtrack.entity.*;
import com.healthtrack.repository.*;
import com.healthtrack.security.UserPrincipal;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final BillRepository billRepository;
    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final LabTestOrderRepository labTestOrderRepository;
    private final DispensationRecordRepository dispensationRecordRepository;
    private final MedicineStockRepository medicineStockRepository;
    private final BillingTransactionRepository billingTransactionRepository;
    private final MeterRegistry meterRegistry;

    @Transactional(readOnly = true)
    @Cacheable(value = "analytics", key = "#hospitalId + '_revenue_' + #months")
    public Map<String, Object> getRevenueAnalytics(Long hospitalId, int months) {
        LocalDateTime since = YearMonth.now().minusMonths(months).atDay(1).atStartOfDay();
        List<Object[]> raw = billRepository.sumNetPayableByHospitalAndMonth(hospitalId, since);
        List<Map<String, Object>> series = new ArrayList<>();

        Map<String, BigDecimal> monthlyMap = new LinkedHashMap<>();
        for (int i = months - 1; i >= 0; i--) {
            YearMonth ym = YearMonth.now().minusMonths(i);
            monthlyMap.put(ym.toString(), BigDecimal.ZERO);
        }
        for (Object[] row : raw) {
            String key = row[1] + "-" + String.format("%02d", row[0]);
            BigDecimal amount = (BigDecimal) row[2];
            monthlyMap.put(key, amount);
        }

        BigDecimal totalRevenue = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> entry : monthlyMap.entrySet()) {
            Map<String, Object> point = new HashMap<>();
            point.put("month", entry.getKey());
            point.put("revenue", entry.getValue());
            series.add(point);
            totalRevenue = totalRevenue.add(entry.getValue());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("monthlyRevenue", series);
        result.put("totalRevenue", totalRevenue);

        BigDecimal averageRevenue = months > 0
                ? totalRevenue.divide(BigDecimal.valueOf(months), BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO;
        result.put("averageMonthlyRevenue", averageRevenue);
        result.put("periodMonths", months);

        return result;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "analytics", key = "#hospitalId + '_topDoctors_' + #limit")
    public List<Map<String, Object>> getTopDoctors(Long hospitalId, int limit) {
        List<Object[]> raw = doctorRepository.findTopByHospitalIdOrderByAppointmentCountDesc(
                hospitalId, org.springframework.data.domain.PageRequest.of(0, limit));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : raw) {
            Map<String, Object> doc = new HashMap<>();
            doc.put("name", row[0]);
            doc.put("appointmentCount", row[1]);
            result.add(doc);
        }
        return result;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "analytics", key = "#hospitalId + '_labVolume_' + #months")
    public Map<String, Object> getLabVolume(Long hospitalId, int months) {
        LocalDateTime since = YearMonth.now().minusMonths(months).atDay(1).atStartOfDay();

        long totalApproved = labTestOrderRepository.countByHospitalIdAndStatus(hospitalId, LabOrderStatus.APPROVED);
        long pendingApproval = labTestOrderRepository.countByHospitalIdAndStatus(hospitalId, LabOrderStatus.PENDING_APPROVAL);
        long resultReady = labTestOrderRepository.countByHospitalIdAndStatus(hospitalId, LabOrderStatus.RESULT_ENTERED);
        long sampleCollected = labTestOrderRepository.countByHospitalIdAndStatus(hospitalId, LabOrderStatus.SAMPLE_COLLECTED);
        long ordered = labTestOrderRepository.countByHospitalIdAndStatus(hospitalId, LabOrderStatus.ORDERED);

        List<Map<String, Object>> monthlyTrend = getLabMonthlyTrend(hospitalId, months);

        Map<String, Object> result = new HashMap<>();
        result.put("totalApproved", totalApproved);
        result.put("pendingApproval", pendingApproval);
        result.put("resultReady", resultReady);
        result.put("sampleCollected", sampleCollected);
        result.put("ordered", ordered);
        result.put("monthlyTrend", monthlyTrend);
        return result;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "analytics", key = "#hospitalId + '_pharmacySales_' + #months")
    public Map<String, Object> getPharmacySales(Long hospitalId, int months) {
        long lowStockCount = medicineStockRepository.countLowStockByHospitalId(hospitalId);
        long totalMedicines = medicineStockRepository.findByHospitalId(hospitalId).size();
        long totalDispensed = dispensationRecordRepository.countByHospitalId(hospitalId);

        LocalDateTime since = YearMonth.now().minusMonths(months).atDay(1).atStartOfDay();
        List<Map<String, Object>> salesTrend = getPharmacyMonthlySales(hospitalId, since);

        Map<String, Object> result = new HashMap<>();
        result.put("totalMedicines", totalMedicines);
        result.put("lowStockItems", lowStockCount);
        result.put("totalDispensed", totalDispensed);
        result.put("stockHealthPercent", totalMedicines > 0
                ? Math.round((double) (totalMedicines - lowStockCount) / totalMedicines * 100) : 0);
        result.put("monthlySalesTrend", salesTrend);
        return result;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "analytics", key = "#hospitalId + '_dashboard'")
    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "analytics", fallbackMethod = "fallbackGetDashboardSummary")
    public Map<String, Object> getDashboardSummary(Long hospitalId) {
        long todayAppointments = appointmentRepository.countByHospitalIdAndAppointmentDate(
                hospitalId, java.time.LocalDate.now());
        long activeAdmissions = 0;
        long pendingLabOrders = labTestOrderRepository.countByHospitalIdAndStatus(hospitalId, LabOrderStatus.ORDERED);
        long lowStockItems = medicineStockRepository.countLowStockByHospitalId(hospitalId);

        Map<String, Object> summary = new HashMap<>();
        summary.put("todayAppointments", todayAppointments);
        summary.put("activeAdmissions", activeAdmissions);
        summary.put("pendingLabOrders", pendingLabOrders);
        summary.put("lowStockItems", lowStockItems);
        return summary;
    }

    public Map<String, Object> fallbackGetDashboardSummary(Long hospitalId, Throwable t) {
        meterRegistry.counter("fallback.invoked.count", "service", "AnalyticsService", "method", "getDashboardSummary").increment();
        Map<String, Object> summary = new HashMap<>();
        summary.put("todayAppointments", 0);
        summary.put("activeAdmissions", 0);
        summary.put("pendingLabOrders", 0);
        summary.put("lowStockItems", 0);
        summary.put("status", "Degraded Mode - Data Unavailable");
        summary.put("degraded", true);
        summary.put("lastUpdated", java.time.OffsetDateTime.now());
        return summary;
    }

    private List<Map<String, Object>> getLabMonthlyTrend(Long hospitalId, int months) {
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = months - 1; i >= 0; i--) {
            YearMonth ym = YearMonth.now().minusMonths(i);
            LocalDateTime start = ym.atDay(1).atStartOfDay();
            LocalDateTime end = ym.atEndOfMonth().atTime(23, 59, 59);

            Map<String, Object> point = new HashMap<>();
            point.put("month", ym.toString());
            point.put("count", labTestOrderRepository.countByHospitalIdAndCreatedAtBetween(hospitalId, start, end));
            trend.add(point);
        }
        return trend;
    }

    private List<Map<String, Object>> getPharmacyMonthlySales(Long hospitalId, LocalDateTime since) {
        List<DispensationRecord> records = dispensationRecordRepository
                .findByHospitalIdAndDispensedAtAfter(hospitalId, since);

        Map<YearMonth, Long> grouped = records.stream()
                .collect(Collectors.groupingBy(
                        r -> YearMonth.from(r.getDispensedAt()),
                        Collectors.counting()));

        List<Map<String, Object>> trend = new ArrayList<>();
        YearMonth start = YearMonth.from(since);
        YearMonth end = YearMonth.now();
        for (YearMonth ym = start; !ym.isAfter(end); ym = ym.plusMonths(1)) {
            Map<String, Object> point = new HashMap<>();
            point.put("month", ym.toString());
            point.put("count", grouped.getOrDefault(ym, 0L));
            trend.add(point);
        }
        return trend;
    }
}

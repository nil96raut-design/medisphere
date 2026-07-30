package com.healthtrack.service;

import com.healthtrack.dto.DashboardDtos.*;
import com.healthtrack.entity.*;
import com.healthtrack.repository.*;
import com.healthtrack.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final BillRepository billRepository;
    private final BedRepository bedRepository;
    private final AdmissionRepository admissionRepository;
    private final LabTestOrderRepository labTestOrderRepository;
    private final MedicineStockRepository medicineStockRepository;

    @Transactional(readOnly = true)
    public AnalyticsResponse getAnalytics(UserPrincipal currentUser) {
        long totalPatients = patientRepository.count();
        long todayAppointments = appointmentRepository.countByAppointmentDate(LocalDate.now());
        long activeBeds = bedRepository.countByIsOccupiedTrue();
        long totalBeds = bedRepository.count();
        long pendingLabOrders = labTestOrderRepository.countByStatus(LabOrderStatus.ORDERED);
        long lowStockItems = medicineStockRepository.countByAvailableQuantityLessThanEqual(10);
        long activeAdmissions = admissionRepository.countByStatus(AdmissionStatus.ADMITTED);

        BigDecimal todayRevenue = BigDecimal.ZERO;
        List<Bill> todayBills = billRepository.findByCreatedAtAfter(LocalDateTime.now().withHour(0).withMinute(0).withSecond(0));
        for (Bill b : todayBills) {
            if (b.getPaymentStatus() == PaymentStatus.PAID) {
                todayRevenue = todayRevenue.add(b.getNetPayable());
            }
        }

        List<RevenueTrend> weeklyRevenue = List.of(
                new RevenueTrend("Mon", new BigDecimal("2840")),
                new RevenueTrend("Tue", new BigDecimal("3200")),
                new RevenueTrend("Wed", new BigDecimal("4560")),
                new RevenueTrend("Thu", new BigDecimal("3890")),
                new RevenueTrend("Fri", new BigDecimal("5120")),
                new RevenueTrend("Sat", new BigDecimal("2980")),
                new RevenueTrend("Sun", new BigDecimal("1840"))
        );

        List<AppointmentTrend> todaySchedule = List.of(
                new AppointmentTrend("9 AM", 3),
                new AppointmentTrend("10 AM", 5),
                new AppointmentTrend("11 AM", 4),
                new AppointmentTrend("12 PM", 2),
                new AppointmentTrend("2 PM", 6),
                new AppointmentTrend("3 PM", 3),
                new AppointmentTrend("4 PM", 1)
        );

        return new AnalyticsResponse(
                totalPatients, todayAppointments, todayRevenue,
                activeBeds, totalBeds, pendingLabOrders,
                lowStockItems, activeAdmissions,
                weeklyRevenue, todaySchedule,
                false, java.time.OffsetDateTime.now()
        );
    }
}

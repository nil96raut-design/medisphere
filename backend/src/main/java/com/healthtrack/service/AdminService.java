package com.healthtrack.service;

import com.healthtrack.dto.AdminDtos.*;
import com.healthtrack.dto.AppointmentDtos;
import com.healthtrack.dto.BillingDtos;
import com.healthtrack.dto.EmrDtos;
import com.healthtrack.dto.LabDtos;
import com.healthtrack.dto.PatientDtos;
import com.healthtrack.entity.*;
import com.healthtrack.repository.*;
import com.healthtrack.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final BillRepository billRepository;
    private final BedRepository bedRepository;
    private final AdmissionRepository admissionRepository;
    private final LabTestOrderRepository labTestOrderRepository;
    private final MedicineStockRepository medicineStockRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final DispensationRecordRepository dispensationRecordRepository;
    private final HospitalRepository hospitalRepository;

    @Transactional(readOnly = true)
    public DashboardOverview getDashboardOverview(UserPrincipal currentUser) {
        Long hospitalId = currentUser.getHospitalId();
        LocalDate today = LocalDate.now();
        LocalDateTime monthStart = YearMonth.now().atDay(1).atStartOfDay();
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = today.atTime(23, 59, 59);

        long patientsToday = patientRepository.countByHospitalIdAndCreatedAtBetween(hospitalId, dayStart, dayEnd);
        long patientsMonth = patientRepository.countByHospitalIdAndCreatedAtBetween(hospitalId, monthStart, LocalDateTime.now());
        long appointmentsToday = appointmentRepository.countByHospitalIdAndAppointmentDate(hospitalId, today);

        List<Bill> todayBills = billRepository.findByHospitalIdAndCreatedAtBetween(hospitalId, dayStart, dayEnd);
        BigDecimal revenueToday = todayBills.stream()
                .filter(b -> b.getPaymentStatus() == PaymentStatus.PAID)
                .map(Bill::getNetPayable)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Bill> monthBills = billRepository.findByHospitalIdAndCreatedAtBetween(hospitalId, monthStart, LocalDateTime.now());
        BigDecimal revenueMonth = monthBills.stream()
                .filter(b -> b.getPaymentStatus() == PaymentStatus.PAID)
                .map(Bill::getNetPayable)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalBeds = bedRepository.count();
        long occupiedBeds = bedRepository.countByIsOccupiedTrue();
        double bedOccupancy = totalBeds > 0 ? (double) occupiedBeds / totalBeds * 100 : 0;

        long completedLabsToday = labTestOrderRepository.countByHospitalIdAndStatusUpdatedAtBetween(
                hospitalId, LabOrderStatus.APPROVED, dayStart, dayEnd);

        long pendingBills = billRepository.countByHospitalIdAndPaymentStatus(hospitalId, PaymentStatus.PENDING);

        return new DashboardOverview(patientsToday, patientsMonth, appointmentsToday,
                revenueToday, revenueMonth, bedOccupancy, completedLabsToday, pendingBills);
    }

    @Transactional(readOnly = true)
    public List<UserActivity> getUserActivity(UserPrincipal currentUser) {
        Long hospitalId = currentUser.getHospitalId();
        List<User> users = userRepository.findByHospitalId(hospitalId);

        return users.stream().map(u -> {
            List<AuditLog> logs = auditLogRepository.findByUserId(u.getId());
            OffsetDateTime lastActive = logs.isEmpty() ? null : logs.get(logs.size() - 1).getTimestamp();
            return new UserActivity(u.getId(), u.getFullName(), u.getEmail(),
                    u.getRole().name(), true, lastActive, logs.size());
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<LoginHistory> getLoginHistory(Long hospitalId, int page, int size) {
        return auditLogRepository.search(hospitalId, "LOGIN", null, null, null,
                        PageRequest.of(page, Math.min(size, 100)))
                .map(log -> new LoginHistory(log.getUserId(), "", log.getAction(), log.getDetails(), log.getTimestamp()));
    }

    @Transactional
    public void deactivateUser(Long userId) {
        userRepository.deleteById(userId);
    }

    @Transactional
    public void updateUserRole(Long userId, Role newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.setRole(newRole);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public PatientFullHistory getPatientFullHistory(Long patientId, UserPrincipal currentUser) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"));

        PatientDtos.PatientResponse patientResp = new PatientDtos.PatientResponse(
                patient.getId(), patient.getFirstName(), patient.getLastName(),
                patient.getGender(), patient.getDateOfBirth(), patient.getPhoneNumber(),
                patient.getEmail(), patient.getEmergencyContact(),
                patient.getPolicyNumber(), false, java.time.OffsetDateTime.now());

        List<AppointmentDtos.AppointmentResponse> appointments = appointmentRepository.findByPatientIdWithDoctor(patientId)
                .stream().map(a -> new AppointmentDtos.AppointmentResponse(
                        a.getId(), a.getPatient().getId(),
                        a.getPatient().getFirstName() + " " + a.getPatient().getLastName(),
                        a.getDoctor().getId(), a.getDoctor().getUser().getFullName(),
                        a.getAppointmentDate(), a.getStartTime(), a.getEndTime(),
                        a.getStatus(), a.getTokenNumber()))
                .collect(Collectors.toList());

        List<BillingDtos.BillResponse> bills = billRepository.findByPatientId(patientId)
                .stream().map(b -> new BillingDtos.BillResponse(
                        b.getId(), b.getPatient().getId(),
                        b.getPatient().getFirstName() + " " + b.getPatient().getLastName(),
                        b.getTotalAmount(), b.getDiscountAmount(),
                        b.getInsuranceCoveredAmount(), b.getNetPayable(),
                        b.getPaymentStatus().name(),
                        b.getPaymentMode() != null ? b.getPaymentMode().name() : null,
                        b.getCreatedAt(),
                        b.getRefundReason(), b.getRefundedAt(), b.getRefundedAmount()))
                .collect(Collectors.toList());

        List<LabDtos.LabOrderResponse> labOrders = labTestOrderRepository.findByPatientIdOrderByCreatedAtDesc(patientId)
                .stream().map(this::mapToLabOrderResponse)
                .collect(Collectors.toList());

        return new PatientFullHistory(patientResp, appointments, bills, labOrders, List.of());
    }

    @Transactional(readOnly = true)
    public List<AppointmentQueueEntry> getAppointmentQueue(Long hospitalId) {
        LocalDate today = LocalDate.now();
        List<Appointment> appointments = appointmentRepository.findByHospitalIdAndAppointmentDateOrderByStartTimeAsc(hospitalId, today);
        return appointments.stream().map(a -> new AppointmentQueueEntry(
                a.getId(), a.getPatient().getId(),
                a.getPatient().getFirstName() + " " + a.getPatient().getLastName(),
                a.getDoctor().getId(), a.getDoctor().getUser().getFullName(),
                a.getAppointmentDate(),
                a.getStartTime() != null ? a.getAppointmentDate().atTime(a.getStartTime()) : null,
                a.getEndTime() != null ? a.getAppointmentDate().atTime(a.getEndTime()) : null,
                a.getStatus().name(), a.getTokenNumber(), a.getPriority() != null ? a.getPriority() : 0
        )).collect(Collectors.toList());
    }

    @Transactional
    public BulkCancelResponse bulkCancelAppointments(BulkCancelRequest request) {
        int cancelled = 0, failed = 0;
        for (Long id : request.appointmentIds()) {
            try {
                Appointment a = appointmentRepository.findById(id)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment " + id + " not found"));
                if (a.getStatus() == AppointmentStatus.CANCELLED || a.getStatus() == AppointmentStatus.COMPLETED) {
                    failed++;
                    continue;
                }
                a.setStatus(AppointmentStatus.CANCELLED);
                appointmentRepository.save(a);
                cancelled++;
            } catch (Exception e) {
                failed++;
            }
        }
        return new BulkCancelResponse(cancelled, failed);
    }

    @Transactional
    public void updateAppointmentPriority(Long appointmentId, PriorityUpdateRequest request) {
        Appointment a = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));
        a.setPriority(request.priority());
        appointmentRepository.save(a);
    }

    @Transactional(readOnly = true)
    public List<BedStatusResponse> getBedStatus(Long hospitalId) {
        return bedRepository.findByHospitalIdOrderByWardNameAscBedNumberAsc(hospitalId).stream()
                .map(b -> {
                    String patientName = "";
                    Long patientId = null;
                    if (Boolean.TRUE.equals(b.getIsOccupied())) {
                        Optional<Admission> active = admissionRepository.findByBedIdAndStatus(b.getId(), AdmissionStatus.ADMITTED);
                        if (active.isPresent()) {
                            patientId = active.get().getPatient().getId();
                            patientName = active.get().getPatient().getFirstName() + " " + active.get().getPatient().getLastName();
                        }
                    }
                    return new BedStatusResponse(b.getId(), b.getBedNumber(), b.getWardName(),
                            Boolean.TRUE.equals(b.getIsOccupied()), b.getChargePerDay(), patientId, patientName);
                }).collect(Collectors.toList());
    }

    @Transactional
    public void transferBed(Long fromBedId, Long toBedId) {
        Bed from = bedRepository.findById(fromBedId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Source bed not found"));
        Bed to = bedRepository.findById(toBedId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Target bed not found"));

        if (!Boolean.TRUE.equals(from.getIsOccupied())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Source bed not occupied");
        if (Boolean.TRUE.equals(to.getIsOccupied())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target bed already occupied");

        Admission admission = admissionRepository.findByBedIdAndStatus(fromBedId, AdmissionStatus.ADMITTED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No active admission on source bed"));

        admission.setBed(to);
        admissionRepository.save(admission);

        from.setIsOccupied(false);
        to.setIsOccupied(true);
        bedRepository.save(from);
        bedRepository.save(to);
    }

    @Transactional(readOnly = true)
    public RevenueSummary getRevenueSummary(Long hospitalId) {
        List<Bill> paidBills = billRepository.findByHospitalIdAndPaymentStatus(hospitalId, PaymentStatus.PAID);
        BigDecimal totalRevenue = paidBills.stream().map(Bill::getNetPayable).reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDateTime monthStart = YearMonth.now().atDay(1).atStartOfDay();
        BigDecimal monthlyRevenue = paidBills.stream()
                .filter(b -> b.getCreatedAt() != null && b.getCreatedAt().toLocalDateTime().isAfter(monthStart))
                .map(Bill::getNetPayable).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal pendingAmount = billRepository.findByHospitalIdAndPaymentStatus(hospitalId, PaymentStatus.PENDING)
                .stream().map(Bill::getNetPayable).reduce(BigDecimal.ZERO, BigDecimal::add);

        long paidCount = paidBills.size();
        long pendingCount = billRepository.countByHospitalIdAndPaymentStatus(hospitalId, PaymentStatus.PENDING);
        long refundedCount = billRepository.countByHospitalIdAndPaymentStatus(hospitalId, PaymentStatus.REFUNDED);

        return new RevenueSummary(totalRevenue, monthlyRevenue, pendingAmount, paidCount, pendingCount, refundedCount);
    }

    @Transactional(readOnly = true)
    public List<RevenueTrend> getRevenueTrends(Long hospitalId, int months) {
        LocalDateTime since = YearMonth.now().minusMonths(months).atDay(1).atStartOfDay();
        List<Object[]> raw = billRepository.sumNetPayableByHospitalAndMonth(hospitalId, since);

        Map<YearMonth, BigDecimal> monthlyMap = new LinkedHashMap<>();
        Map<YearMonth, Long> countMap = new LinkedHashMap<>();
        for (int i = months - 1; i >= 0; i--) {
            YearMonth ym = YearMonth.now().minusMonths(i);
            monthlyMap.put(ym, BigDecimal.ZERO);
            countMap.put(ym, 0L);
        }
        for (Object[] row : raw) {
            int month = ((Number) row[0]).intValue();
            int year = ((Number) row[1]).intValue();
            BigDecimal amount = (BigDecimal) row[2];
            YearMonth ym = YearMonth.of(year, month);
            monthlyMap.merge(ym, amount, BigDecimal::add);
            countMap.merge(ym, 1L, Long::sum);
        }
        return monthlyMap.entrySet().stream()
                .map(e -> new RevenueTrend(e.getKey().toString(), e.getValue(), countMap.get(e.getKey())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PendingBill> getPendingBills(Long hospitalId) {
        return billRepository.findByHospitalIdAndPaymentStatus(hospitalId, PaymentStatus.PENDING)
                .stream().map(b -> new PendingBill(b.getId(), b.getPatient().getId(),
                        b.getPatient().getFirstName() + " " + b.getPatient().getLastName(),
                        b.getNetPayable(), b.getCreatedAt(), b.getPatient().getPhoneNumber()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public LabSummary getLabSummary(Long hospitalId) {
        long ordered = labTestOrderRepository.countByHospitalIdAndStatus(hospitalId, LabOrderStatus.ORDERED);
        long sampleCollected = labTestOrderRepository.countByHospitalIdAndStatus(hospitalId, LabOrderStatus.SAMPLE_COLLECTED);
        long resultReady = labTestOrderRepository.countByHospitalIdAndStatus(hospitalId, LabOrderStatus.RESULT_ENTERED);
        long pendingApproval = labTestOrderRepository.countByHospitalIdAndStatus(hospitalId, LabOrderStatus.PENDING_APPROVAL);
        long approved = labTestOrderRepository.countByHospitalIdAndStatus(hospitalId, LabOrderStatus.APPROVED);
        long abnormal = labTestOrderRepository.countAbnormalByHospitalId(hospitalId);
        return new LabSummary(ordered, sampleCollected, resultReady, pendingApproval, approved, abnormal);
    }

    private LabDtos.LabOrderResponse mapToLabOrderResponse(LabTestOrder o) {
        return new LabDtos.LabOrderResponse(
                o.getId(), o.getPatient().getId(),
                o.getPatient().getFirstName() + " " + o.getPatient().getLastName(),
                o.getTestName(),
                o.getRequestedBy() != null ? o.getRequestedBy().getFullName() : null,
                o.getStatus().name(), o.getResultValues(), o.getTechnicianNotes(),
                o.getCompletedAt(), o.getCreatedAt(), o.getSampleCollectedAt(),
                o.getProcessingStartedAt(), o.getProcessingCompletedAt(),
                o.getSampleBarcode(), o.getSampleStorageLocation(),
                o.getCriticalFlag(), o.getTurnaroundMinutes(),
                o.getRetestOf() != null ? o.getRetestOf().getId() : null,
                o.getCorrectionReason(),
                o.getResultEnteredBy() != null ? o.getResultEnteredBy().getFullName() : null,
                o.getPrice(), List.of());
    }

    @Transactional(readOnly = true)
    public List<LabAbnormal> getAbnormalResults(Long hospitalId) {
        return labTestOrderRepository.findAbnormalByHospitalId(hospitalId).stream()
                .map(o -> new LabAbnormal(o.getId(), o.getPatient().getId(),
                        o.getPatient().getFirstName() + " " + o.getPatient().getLastName(),
                        o.getTestName(), o.getResultValues(), o.getStatus().name()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PharmacyLowStock> getLowStockItems(Long hospitalId) {
        return medicineStockRepository.findLowStockByHospitalId(hospitalId).stream()
                .map(m -> new PharmacyLowStock(m.getId(), m.getMedicineName(), m.getBatchNumber(),
                        m.getAvailableQuantity(), m.getReorderLevel(), m.getExpiryDate()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PharmacyExpiring> getExpiringItems(Long hospitalId, int days) {
        LocalDate threshold = LocalDate.now().plusDays(days);
        return medicineStockRepository.findByHospitalIdAndExpiryDateBefore(hospitalId, threshold).stream()
                .map(m -> new PharmacyExpiring(m.getId(), m.getMedicineName(), m.getBatchNumber(),
                        m.getAvailableQuantity(), m.getExpiryDate(),
                        (int) LocalDate.now().until(m.getExpiryDate()).getDays()))
                .sorted(Comparator.comparingInt(PharmacyExpiring::daysUntilExpiry))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PharmacySalesSummary getPharmacySalesSummary(Long hospitalId) {
        long totalDispensed = dispensationRecordRepository.countByHospitalId(hospitalId);
        long lowStock = medicineStockRepository.countLowStockByHospitalId(hospitalId);
        LocalDate expiryThreshold = LocalDate.now().plusDays(30);
        long expiringItems = medicineStockRepository.countByHospitalIdAndExpiryDateBefore(hospitalId, expiryThreshold);
        List<DispensationRecord> monthRecords = dispensationRecordRepository.findByHospitalIdAndDispensedAtAfter(
                hospitalId, YearMonth.now().atDay(1).atStartOfDay());
        BigDecimal monthlySales = monthRecords.stream()
                .map(d -> d.getTotalPrice() != null ? d.getTotalPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new PharmacySalesSummary(totalDispensed, lowStock, expiringItems, monthlySales);
    }

    @Transactional(readOnly = true)
    public HospitalSettings getHospitalSettings(Long hospitalId) {
        Hospital h = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hospital not found"));
        return new HospitalSettings(h.getName(), h.getContactEmail(),
                h.getSubscriptionTier() != null ? h.getSubscriptionTier().name() : null,
                h.getSubscriptionStatus() != null ? h.getSubscriptionStatus().name() : null,
                h.getTrialEndDate(), BigDecimal.valueOf(500), 10);
    }

    @Transactional
    public void updateHospitalSettings(Long hospitalId, HospitalSettingsUpdate update) {
        Hospital h = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hospital not found"));
        if (update.hospitalName() != null) h.setName(update.hospitalName());
        if (update.contactEmail() != null) h.setContactEmail(update.contactEmail());
        hospitalRepository.save(h);
    }
}
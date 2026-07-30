package com.healthtrack.service;

import com.healthtrack.dto.BillingDtos.*;
import com.healthtrack.entity.*;
import com.healthtrack.repository.*;
import com.healthtrack.security.TenantValidator;
import com.healthtrack.security.UserPrincipal;
import com.healthtrack.event.EventPublisher;
import com.healthtrack.util.DateUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.dao.DataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BillingService {

    private final BillingTransactionRepository billingTransactionRepository;
    private final BillRepository billRepository;
    private final BilledItemRepository billedItemRepository;
    private final PaymentRepository paymentRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final AdmissionRepository admissionRepository;
    private final DoctorRepository doctorRepository;
    private final DispensationRecordRepository dispensationRecordRepository;
    private final LabTestOrderRepository labTestOrderRepository;
    private final TenantValidator tenantValidator;
    private final EventPublisher eventPublisher;
    private final WriteBufferService writeBufferService;

    @Transactional(readOnly = true)
    public CalculateResponse calculate(Long patientId, UserPrincipal currentUser) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"));
        tenantValidator.validateHospitalAccess(patient.getHospital().getId(), currentUser.getHospitalId());

        List<LineItem> items = new ArrayList<>();

        List<Appointment> appointments = appointmentRepository.findByPatientId(patientId);
        for (Appointment a : appointments) {
            if (billedItemRepository.existsBySourceTypeAndSourceId("APPOINTMENT", a.getId())) {
                continue;
            }
            Doctor doctor = a.getDoctor();
            if (doctor != null && doctor.getConsultationFee() != null && doctor.getUser() != null) {
                items.add(new LineItem(
                        "Consultation - " + doctor.getUser().getFullName() + " (" + a.getAppointmentDate() + ")",
                        doctor.getConsultationFee(), 1, doctor.getConsultationFee(),
                        "APPOINTMENT", a.getId()));
            }
        }

        List<Admission> admissions = admissionRepository.findByPatientIdOrderByAdmissionDateDesc(patientId);
        for (Admission adm : admissions) {
            if (adm.getBed() != null) {
                long days = ChronoUnit.DAYS.between(
                        adm.getAdmissionDate(),
                        adm.getDischargeDate() != null ? adm.getDischargeDate() : LocalDate.now()) + 1;
                BigDecimal totalRent = adm.getBed().getChargePerDay().multiply(BigDecimal.valueOf(days));
                items.add(new LineItem(
                        "Bed Rent - " + adm.getBed().getWardName() + " #" + adm.getBed().getBedNumber()
                                + " (" + days + " days @ $" + adm.getBed().getChargePerDay() + "/day)",
                        adm.getBed().getChargePerDay(), (int) days, totalRent,
                        "ADMISSION", adm.getId()));
            }
        }

        List<DispensationRecord> dispensations = dispensationRecordRepository
                .findByPatientIdAndBillingStatus(patientId, BillingStatus.PENDING);
        for (DispensationRecord d : dispensations) {
            if (billedItemRepository.existsBySourceTypeAndSourceId("DISPENSATION", d.getId())) {
                continue;
            }
            items.add(new LineItem(
                    "Pharmacy - " + d.getMedicineName() + " x" + d.getQuantityDispensed(),
                    d.getUnitPrice(), d.getQuantityDispensed(), d.getTotalPrice(),
                    "DISPENSATION", d.getId()));
        }

        List<LabTestOrder> labOrders = labTestOrderRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
        for (LabTestOrder lo : labOrders) {
            if (billedItemRepository.existsBySourceTypeAndSourceId("LAB_ORDER", lo.getId())) {
                continue;
            }
            if ((lo.getStatus() == LabOrderStatus.RESULT_ENTERED || lo.getStatus() == LabOrderStatus.APPROVED) && lo.getPrice() != null
                    && lo.getPrice().compareTo(BigDecimal.ZERO) > 0) {
                items.add(new LineItem(
                        "Lab - " + lo.getTestName(),
                        lo.getPrice(), 1, lo.getPrice(),
                        "LAB_ORDER", lo.getId()));
            }
        }

        BigDecimal totalAmount = items.stream().map(LineItem::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CalculateResponse(patientId, patient.getFirstName() + " " + patient.getLastName(),
                items, totalAmount, BigDecimal.ZERO, BigDecimal.ZERO, totalAmount);
    }

    @Transactional(timeout = 5)
    @Retryable(
            retryFor = {DataAccessException.class, org.hibernate.exception.JDBCConnectionException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2.0)
    )
    public BillResponse settle(SettleRequest request, UserPrincipal currentUser) {
        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"));
        tenantValidator.validateHospitalAccess(patient.getHospital().getId(), currentUser.getHospitalId());

        String idempotencyKey = request.idempotencyKey() != null ? request.idempotencyKey() : UUID.randomUUID().toString();

        if (billRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Duplicate settlement detected (idempotency key already used)");
        }

        CalculateResponse calc = calculate(request.patientId(), currentUser);
        BigDecimal discount = request.discountAmount() != null ? request.discountAmount() : BigDecimal.ZERO;
        BigDecimal insurance = request.insuranceCoveredAmount() != null ? request.insuranceCoveredAmount() : BigDecimal.ZERO;
        BigDecimal netPayable = calc.totalAmount().subtract(discount).subtract(insurance);

        PaymentMode mode;
        try {
            mode = PaymentMode.valueOf(request.paymentMode());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid payment mode");
        }

        BigDecimal amountPaid = request.amountPaid() != null ? request.amountPaid() : netPayable;
        PaymentStatus initialStatus = amountPaid.compareTo(BigDecimal.ZERO) == 0 ? PaymentStatus.DRAFT :
                (amountPaid.compareTo(netPayable) >= 0 ? PaymentStatus.PAID : PaymentStatus.PARTIALLY_PAID);

        Bill bill = Bill.builder()
                .hospital(patient.getHospital())
                .patient(patient)
                .idempotencyKey(idempotencyKey)
                .totalAmount(calc.totalAmount())
                .discountAmount(discount)
                .insuranceCoveredAmount(insurance)
                .netPayable(netPayable)
                .paymentStatus(initialStatus)
                .paymentMode(mode)
                .build();

        bill = billRepository.save(bill);

        if (amountPaid.compareTo(BigDecimal.ZERO) > 0) {
            Payment payment = Payment.builder()
                    .bill(bill)
                    .hospital(patient.getHospital())
                    .amountPaid(amountPaid)
                    .paymentMode(mode)
                    .transactionReference(request.transactionReference() != null ? request.transactionReference() : "AUTO-" + UUID.randomUUID().toString().substring(0, 8))
                    .remarks("Initial Settlement")
                    .build();
            paymentRepository.save(payment);
        }

        List<DispensationRecord> pending = dispensationRecordRepository
                .findByPatientIdAndBillingStatus(patient.getId(), BillingStatus.PENDING);
        for (DispensationRecord d : pending) {
            if (!billedItemRepository.existsBySourceTypeAndSourceId("DISPENSATION", d.getId())) {
                d.setBillingStatus(BillingStatus.PAID);
            }
        }
        dispensationRecordRepository.saveAll(pending);

        for (LineItem item : calc.items()) {
            if (item.sourceType() == null || item.sourceId() == null) continue;
            billedItemRepository.save(BilledItem.builder()
                    .hospital(patient.getHospital())
                    .patient(patient)
                    .bill(bill)
                    .sourceType(item.sourceType())
                    .sourceId(item.sourceId())
                    .amount(item.total())
                    .build());
        }

        eventPublisher.publish("BILL_SETTLED", currentUser.getHospitalId(),
                Map.of("billId", bill.getId(), "patientId", patient.getId(),
                       "hospitalId", currentUser.getHospitalId(), "netPayable", netPayable));

        return mapToResponse(bill);
    }

    @Recover
    public BillResponse recoverSettle(Exception e, SettleRequest request, UserPrincipal currentUser) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("request", request);
        payload.put("userId", currentUser.getUser().getId());
        payload.put("hospitalId", currentUser.getHospitalId());

        writeBufferService.queueFailedWrite("BILL_SETTLE", payload, QueuePriority.HIGH);

        return new BillResponse(
                -1L, request.patientId(), "Patient Name Unavailable",
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                PaymentStatus.DRAFT.name(), request.paymentMode(),
                OffsetDateTime.now(), null, null, null);
    }

    @Transactional(timeout = 5)
    public RefundResponse refund(Long billId, RefundRequest request, UserPrincipal currentUser) {
        Bill bill = billRepository.findByIdLocked(billId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bill not found"));
        tenantValidator.validateHospitalAccess(bill.getHospital().getId(), currentUser.getHospitalId());

        PaymentStatus current = bill.getPaymentStatus();
        if (current != PaymentStatus.PAID && current != PaymentStatus.PARTIALLY_PAID) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only PAID or PARTIALLY_PAID bills can be refunded (current: " + current + ")");
        }

        BigDecimal refundAmount = request.amount() != null ? request.amount() : bill.getNetPayable();
        BigDecimal maxRefundable = bill.getNetPayable().subtract(bill.getRefundedAmount());

        if (refundAmount.compareTo(maxRefundable) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Refund amount $" + refundAmount + " exceeds remaining refundable amount $" + maxRefundable);
        }

        BigDecimal totalRefunded = bill.getRefundedAmount().add(refundAmount);
        bill.setRefundedAmount(totalRefunded);
        bill.setRefundReason(request.reason());
        bill.setRefundedAt(DateUtils.nowUtc());
        bill.setUpdatedAt(DateUtils.nowUtc());

        if (totalRefunded.compareTo(bill.getNetPayable()) >= 0) {
            bill.setPaymentStatus(PaymentStatus.REFUNDED);
        } else {
            bill.setPaymentStatus(PaymentStatus.PARTIALLY_PAID);
        }

        bill = billRepository.save(bill);

        List<BilledItem> billedItems = billedItemRepository.findByBillId(bill.getId());
        for (BilledItem bi : billedItems) {
            if ("DISPENSATION".equals(bi.getSourceType())) {
                dispensationRecordRepository.findById(bi.getSourceId()).ifPresent(d -> {
                    d.setBillingStatus(BillingStatus.PENDING);
                    dispensationRecordRepository.save(d);
                });
            }
        }

        return new RefundResponse(
                bill.getId(),
                bill.getPaymentStatus().name(),
                bill.getRefundReason(),
                bill.getRefundedAt(),
                bill.getRefundedAmount());
    }

    @Transactional(timeout = 5)
    public BillResponse pay(Long billId, PaymentRequest request, UserPrincipal currentUser) {
        Bill bill = billRepository.findByIdLocked(billId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bill not found"));
        tenantValidator.validateHospitalAccess(bill.getHospital().getId(), currentUser.getHospitalId());

        if (bill.getPaymentStatus() == PaymentStatus.PAID || bill.getPaymentStatus() == PaymentStatus.VOIDED || bill.getPaymentStatus() == PaymentStatus.REFUNDED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bill is already in status: " + bill.getPaymentStatus());
        }

        PaymentMode mode;
        try {
            mode = PaymentMode.valueOf(request.paymentMode());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid payment mode");
        }

        Payment payment = Payment.builder()
                .bill(bill)
                .hospital(bill.getHospital())
                .amountPaid(request.amount())
                .paymentMode(mode)
                .transactionReference(request.transactionReference())
                .remarks(request.remarks())
                .build();
        paymentRepository.save(payment);

        List<Payment> allPayments = paymentRepository.findByBillId(bill.getId());
        BigDecimal totalPaid = allPayments.stream()
                .map(Payment::getAmountPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalPaid.compareTo(bill.getNetPayable()) >= 0) {
            bill.setPaymentStatus(PaymentStatus.PAID);
        } else {
            bill.setPaymentStatus(PaymentStatus.PARTIALLY_PAID);
        }

        bill = billRepository.save(bill);
        return mapToResponse(bill);
    }

    @Transactional(readOnly = true)
    public List<BillResponse> getPatientBills(Long patientId, UserPrincipal currentUser) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"));
        tenantValidator.validateHospitalAccess(patient.getHospital().getId(), currentUser.getHospitalId());
        return billRepository.findByPatientId(patientId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    private BillResponse mapToResponse(Bill b) {
        return new BillResponse(
                b.getId(), b.getPatient().getId(),
                b.getPatient().getFirstName() + " " + b.getPatient().getLastName(),
                b.getTotalAmount(), b.getDiscountAmount(),
                b.getInsuranceCoveredAmount(), b.getNetPayable(),
                b.getPaymentStatus().name(),
                b.getPaymentMode() != null ? b.getPaymentMode().name() : null,
                b.getCreatedAt(),
                b.getRefundReason(),
                b.getRefundedAt(),
                b.getRefundedAmount());
    }
}

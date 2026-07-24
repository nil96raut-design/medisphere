package com.healthtrack.service;

import com.healthtrack.dto.BillingDtos.*;
import com.healthtrack.entity.*;
import com.healthtrack.repository.*;
import com.healthtrack.security.TenantValidator;
import com.healthtrack.security.UserPrincipal;
import com.healthtrack.util.DateUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BillingService {

    private final BillingTransactionRepository billingTransactionRepository;
    private final BillRepository billRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final AdmissionRepository admissionRepository;
    private final DoctorRepository doctorRepository;
    private final DispensationRecordRepository dispensationRecordRepository;
    private final LabTestOrderRepository labTestOrderRepository;
    private final TenantValidator tenantValidator;

    @Transactional(readOnly = true)
    public CalculateResponse calculate(Long patientId, UserPrincipal currentUser) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"));
        tenantValidator.validateHospitalAccess(patient.getHospital().getId(), currentUser.getHospitalId());

        List<LineItem> items = new ArrayList<>();

        List<Appointment> appointments = appointmentRepository.findByPatientId(patientId);
        for (Appointment a : appointments) {
            Doctor doctor = a.getDoctor();
            if (doctor != null && doctor.getConsultationFee() != null && doctor.getUser() != null) {
                items.add(new LineItem(
                        "Consultation - " + doctor.getUser().getFullName() + " (" + a.getAppointmentDate() + ")",
                        doctor.getConsultationFee(), 1, doctor.getConsultationFee()));
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
                        adm.getBed().getChargePerDay(), (int) days, totalRent));
            }
        }

        List<DispensationRecord> dispensations = dispensationRecordRepository
                .findByPatientIdAndBillingStatus(patientId, BillingStatus.PENDING);
        for (DispensationRecord d : dispensations) {
            items.add(new LineItem(
                    "Pharmacy - " + d.getMedicineName() + " x" + d.getQuantityDispensed(),
                    d.getUnitPrice(), d.getQuantityDispensed(), d.getTotalPrice()));
        }

        List<LabTestOrder> labOrders = labTestOrderRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
        for (LabTestOrder lo : labOrders) {
            if (lo.getStatus() == LabOrderStatus.RESULT_READY && lo.getPrice() != null
                    && lo.getPrice().compareTo(BigDecimal.ZERO) > 0) {
                items.add(new LineItem(
                        "Lab - " + lo.getTestName(),
                        lo.getPrice(), 1, lo.getPrice()));
            }
        }

        BigDecimal totalAmount = items.stream().map(LineItem::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CalculateResponse(patientId, patient.getFirstName() + " " + patient.getLastName(),
                items, totalAmount, BigDecimal.ZERO, BigDecimal.ZERO, totalAmount);
    }

    @Transactional
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

        Bill bill = Bill.builder()
                .hospital(patient.getHospital())
                .patient(patient)
                .idempotencyKey(idempotencyKey)
                .totalAmount(calc.totalAmount())
                .discountAmount(discount)
                .insuranceCoveredAmount(insurance)
                .netPayable(netPayable)
                .paymentStatus(PaymentStatus.PAID)
                .paymentMode(mode)
                .build();

        bill = billRepository.save(bill);

        List<DispensationRecord> pending = dispensationRecordRepository
                .findByPatientIdAndBillingStatus(patient.getId(), BillingStatus.PENDING);
        for (DispensationRecord d : pending) {
            d.setBillingStatus(BillingStatus.PAID);
        }
        dispensationRecordRepository.saveAll(pending);

        return mapToResponse(bill);
    }

    private BillResponse mapToResponse(Bill b) {
        return new BillResponse(
                b.getId(), b.getPatient().getId(),
                b.getPatient().getFirstName() + " " + b.getPatient().getLastName(),
                b.getTotalAmount(), b.getDiscountAmount(),
                b.getInsuranceCoveredAmount(), b.getNetPayable(),
                b.getPaymentStatus().name(),
                b.getPaymentMode() != null ? b.getPaymentMode().name() : null,
                b.getCreatedAt());
    }
}

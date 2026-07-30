package com.healthtrack.service;

import com.healthtrack.dto.BillingDtos.*;
import com.healthtrack.entity.*;
import com.healthtrack.repository.*;
import com.healthtrack.security.UserPrincipal;
import com.healthtrack.support.PostgresTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BillingServiceTest extends PostgresTestBase {

    @Autowired private BillingService billingService;
    @Autowired private HospitalRepository hospitalRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private DoctorRepository doctorRepository;
    @Autowired private AppointmentRepository appointmentRepository;
    @Autowired private AdmissionRepository admissionRepository;
    @Autowired private BedRepository bedRepository;

    private Hospital hospital;
    private Patient patient;
    private UserPrincipal staffPrincipal;
    private Doctor doctor;
    private Long doctorId;

    @BeforeEach
    void setUp() {
        hospital = hospitalRepository.save(Hospital.builder()
                .name("Billing Test Hospital").licenseNumber("BILL-" + System.nanoTime())
                .contactEmail("bill@test.com")
                .subscriptionTier(SubscriptionTier.MONTHLY)
                .subscriptionStatus(SubscriptionStatus.ACTIVE).build());

        User staffUser = userRepository.save(User.builder()
                .fullName("Billing Staff").email("staff-bill-" + System.nanoTime() + "@test.com")
                .passwordHash("x").role(Role.RECEPTIONIST).hospital(hospital).build());

        User docUser = userRepository.save(User.builder()
                .fullName("Dr. Biller").email("doc-bill-" + System.nanoTime() + "@test.com")
                .passwordHash("x").role(Role.DOCTOR).hospital(hospital).build());

        staffPrincipal = new UserPrincipal(staffUser);

        patient = patientRepository.save(Patient.builder()
                .hospital(hospital).firstName("Bill").lastName("Patient")
                .phoneNumber("555-BILL-1").build());

        doctor = doctorRepository.save(Doctor.builder()
                .hospital(hospital).user(docUser)
                .specialization("General")
                .consultationFee(new BigDecimal("200"))
                .isAvailable(true).build());
        doctorId = doctor.getId();
    }

    @Test
    void calculate_withAppointment_includesConsultation() {
        appointmentRepository.save(Appointment.builder()
                .hospital(hospital).patient(patient).doctor(doctor)
                .appointmentDate(LocalDate.now())
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(9, 30))
                .status(AppointmentStatus.COMPLETED).build());

        CalculateResponse calc = billingService.calculate(patient.getId(), staffPrincipal);

        assertThat(calc.totalAmount()).isEqualByComparingTo(new BigDecimal("200"));
        assertThat(calc.items()).hasSize(1);
        assertThat(calc.items().get(0).description()).contains("Consultation");
    }

    @Test
    void calculate_withAdmission_includesBedRent() {
        Bed bed = bedRepository.save(Bed.builder()
                .hospital(hospital).wardName("Private").bedNumber("P-1")
                .chargePerDay(new BigDecimal("1000")).isOccupied(true).build());

        admissionRepository.save(Admission.builder()
                .hospital(hospital).patient(patient).doctor(userRepository.findByEmail(
                        userRepository.findAll().stream().filter(u -> u.getRole() == Role.DOCTOR).findFirst().orElseThrow().getEmail()).orElseThrow())
                .bed(bed).admissionDate(LocalDate.now().minusDays(2))
                .status(AdmissionStatus.ADMITTED).build());

        CalculateResponse calc = billingService.calculate(patient.getId(), staffPrincipal);

        assertThat(calc.totalAmount()).isGreaterThan(BigDecimal.ZERO);
        assertThat(calc.items()).anyMatch(i -> i.description().contains("Bed Rent"));
    }

    @Test
    void settle_createsBill() {
        appointmentRepository.save(Appointment.builder()
                .hospital(hospital).patient(patient).doctor(doctor)
                .appointmentDate(LocalDate.now())
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(10, 30))
                .status(AppointmentStatus.COMPLETED).build());

        BillResponse bill = billingService.settle(
                new SettleRequest(patient.getId(), BigDecimal.ZERO, BigDecimal.ZERO, null, "CASH", null, UUID.randomUUID().toString()),
                staffPrincipal);

        assertThat(bill.patientName()).contains("Bill");
        assertThat(bill.paymentStatus()).isEqualTo("PAID");
        assertThat(bill.netPayable()).isEqualByComparingTo(new BigDecimal("200"));
    }

    @Test
    void settle_duplicateIdempotencyKey_throws409() {
        appointmentRepository.save(Appointment.builder()
                .hospital(hospital).patient(patient).doctor(doctor)
                .appointmentDate(LocalDate.now())
                .startTime(LocalTime.of(11, 0))
                .endTime(LocalTime.of(11, 30))
                .status(AppointmentStatus.COMPLETED).build());

        String idempotencyKey = UUID.randomUUID().toString();

        billingService.settle(
                new SettleRequest(patient.getId(), BigDecimal.ZERO, BigDecimal.ZERO, null, "CASH", null, idempotencyKey),
                staffPrincipal);

        assertThatThrownBy(() -> billingService.settle(
                new SettleRequest(patient.getId(), BigDecimal.ZERO, BigDecimal.ZERO, null, "CASH", null, idempotencyKey),
                staffPrincipal))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409 CONFLICT");
    }

    @Test
    void settle_withDiscount_applied() {
        appointmentRepository.save(Appointment.builder()
                .hospital(hospital).patient(patient).doctor(doctor)
                .appointmentDate(LocalDate.now())
                .startTime(LocalTime.of(12, 0))
                .endTime(LocalTime.of(12, 30))
                .status(AppointmentStatus.COMPLETED).build());

        BillResponse bill = billingService.settle(
                new SettleRequest(patient.getId(), new BigDecimal("50"), BigDecimal.ZERO, null, "CARD", null, null),
                staffPrincipal);

        assertThat(bill.discountAmount()).isEqualByComparingTo(new BigDecimal("50"));
        assertThat(bill.netPayable()).isEqualByComparingTo(new BigDecimal("150"));
    }
}

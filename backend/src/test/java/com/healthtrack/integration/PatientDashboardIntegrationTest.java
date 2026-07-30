package com.healthtrack.integration;

import com.healthtrack.dto.AppointmentDtos.AppointmentRequest;
import com.healthtrack.dto.AppointmentDtos.AppointmentResponse;
import com.healthtrack.dto.DoctorDtos.PatientFullProfileResponse;
import com.healthtrack.dto.EmrDtos.CreateMedicalRecordRequest;
import com.healthtrack.dto.EmrDtos.PrescriptionItemRequest;
import com.healthtrack.dto.BillingDtos.*;
import com.healthtrack.entity.*;
import com.healthtrack.repository.*;
import com.healthtrack.security.UserPrincipal;
import com.healthtrack.service.AppointmentService;
import com.healthtrack.service.BillingService;
import com.healthtrack.service.MedicalRecordService;
import com.healthtrack.service.PatientPortalService;
import com.healthtrack.support.PostgresTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PatientDashboardIntegrationTest extends PostgresTestBase {

    @Autowired private PatientPortalService patientPortalService;
    @Autowired private AppointmentService appointmentService;
    @Autowired private MedicalRecordService medicalRecordService;
    @Autowired private BillingService billingService;
    @Autowired private HospitalRepository hospitalRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private DoctorRepository doctorRepository;
    @Autowired private AppointmentRepository appointmentRepository;
    @Autowired private LabTestOrderRepository labTestOrderRepository;
    @Autowired private BillRepository billRepository;

    private Hospital hospital;
    private Patient patient;
    private User patientUser;
    private User doctorUser;
    private Doctor doctor;
    private UserPrincipal patientPrincipal;
    private UserPrincipal adminPrincipal;
    private Long doctorId;

    @BeforeEach
    void setUp() {
        hospital = hospitalRepository.save(Hospital.builder()
                .name("Patient Dashboard Test Hospital").licenseNumber("PD-" + System.nanoTime())
                .contactEmail("pd-test@test.com")
                .subscriptionTier(SubscriptionTier.MONTHLY)
                .subscriptionStatus(SubscriptionStatus.ACTIVE).build());

        patientUser = userRepository.save(User.builder()
                .fullName("Test Patient").email("patient-" + System.nanoTime() + "@test.com")
                .passwordHash("x").role(Role.PATIENT).hospital(hospital).build());

        doctorUser = userRepository.save(User.builder()
                .fullName("Dr. Test").email("doctor-pd-" + System.nanoTime() + "@test.com")
                .passwordHash("x").role(Role.DOCTOR).hospital(hospital).build());

        doctor = doctorRepository.save(Doctor.builder()
                .hospital(hospital).user(doctorUser)
                .specialization("General Medicine").consultationFee(BigDecimal.valueOf(300))
                .isAvailable(true).build());
        doctorId = doctor.getId();

        patient = patientRepository.save(Patient.builder()
                .hospital(hospital).firstName("John").lastName("Patient")
                .phoneNumber("3333333333").email(patientUser.getEmail())
                .gender("Male").dateOfBirth(LocalDate.of(1990, 5, 15))
                .build());

        patientPrincipal = new UserPrincipal(patientUser);
        User adminUser = userRepository.save(User.builder()
                .fullName("Admin PD").email("admin-pd-" + System.nanoTime() + "@test.com")
                .passwordHash("x").role(Role.ADMIN).hospital(hospital).build());
        adminPrincipal = new UserPrincipal(adminUser);
    }

    @Test
    void patientCanViewProfile() {
        PatientFullProfileResponse profile = patientPortalService.getMyProfile(patientPrincipal);
        assertThat(profile.patient()).isNotNull();
        assertThat(profile.patient().firstName()).isEqualTo("John");
        assertThat(profile.patient().phoneNumber()).isEqualTo("3333333333");
    }

    @Test
    void patientCanViewAppointments() {
        // Book an appointment via admin
        AppointmentRequest req = new AppointmentRequest(
                patient.getId(), doctorId, LocalDate.now(),
                LocalTime.of(9, 0), LocalTime.of(9, 30), false);
        appointmentService.bookAppointment(req, adminPrincipal);

        var appointments = patientPortalService.getMyAppointments(PageRequest.of(0, 10), patientPrincipal);
        assertThat(appointments).isNotEmpty();
        assertThat(appointments.getContent().get(0).patientName()).contains("John");
    }

    @Test
    void patientCanViewMedicalRecords() {
        // Create a medical record as doctor
        var presc = List.of(new PrescriptionItemRequest("Paracetamol", "500mg", "TID", "5 days", "After food"));
        CreateMedicalRecordRequest recordReq = new CreateMedicalRecordRequest(
                patient.getId(), null, LocalDate.now(),
                "Headache and fever",
                "Temp 101F, BP normal", "Viral Fever",
                LocalDate.now().plusDays(3), presc, null);
        medicalRecordService.createRecord(recordReq, new UserPrincipal(doctorUser));

        var records = patientPortalService.getMyMedicalRecords(patientPrincipal);
        assertThat(records).hasSize(1);
        assertThat(records.get(0).diagnosis()).isEqualTo("Viral Fever");
        assertThat(records.get(0).prescriptions()).hasSize(1);
        assertThat(records.get(0).prescriptions().get(0).medicineName()).isEqualTo("Paracetamol");
    }

    @Test
    void patientCanViewLabOrders() {
        // Create a lab order
        LabTestOrder labOrder = labTestOrderRepository.save(LabTestOrder.builder()
                .hospital(hospital).patient(patient)
                .testName("Complete Blood Count")
                .requestedBy(doctorUser)
                .status(LabOrderStatus.APPROVED)
                .resultValues("Hb: 14.5, WBC: 7800, Platelets: 250000")
                .price(BigDecimal.valueOf(200))
                .build());

        var labOrders = patientPortalService.getMyLabOrders(patientPrincipal);
        assertThat(labOrders).hasSize(1);
        assertThat(labOrders.get(0).testName()).isEqualTo("Complete Blood Count");
        assertThat(labOrders.get(0).resultValues()).contains("Hb: 14.5");
    }

    @Test
    void patientCanViewBills() {
        // Create a bill
        Bill bill = billRepository.save(Bill.builder()
                .hospital(hospital).patient(patient)
                .idempotencyKey(UUID.randomUUID().toString())
                .totalAmount(BigDecimal.valueOf(1500))
                .discountAmount(BigDecimal.valueOf(100))
                .netPayable(BigDecimal.valueOf(1400))
                .paymentStatus(PaymentStatus.UNPAID)
                .build());

        var bills = patientPortalService.getMyBills(patientPrincipal);
        assertThat(bills).hasSize(1);
        assertThat(bills.get(0).netPayable()).isEqualByComparingTo(BigDecimal.valueOf(1400));
        assertThat(bills.get(0).paymentStatus()).isEqualTo("UNPAID");
    }

    @Test
    void patientCanViewAvailableDoctors() {
        var doctors = patientPortalService.getAvailableDoctors(patientPrincipal);
        assertThat(doctors).isNotEmpty();
        assertThat(doctors.get(0).fullName()).contains("Dr. Test");
    }

    @Test
    void patientOnlySeesTheirOwnData() {
        // Create a different patient
        Patient otherPatient = patientRepository.save(Patient.builder()
                .hospital(hospital).firstName("Other").lastName("Person")
                .phoneNumber("4444444444").email("other-" + System.nanoTime() + "@test.com")
                .build());

        // Create a bill for other patient
        billRepository.save(Bill.builder()
                .hospital(hospital).patient(otherPatient)
                .idempotencyKey(UUID.randomUUID().toString())
                .totalAmount(BigDecimal.valueOf(9999))
                .netPayable(BigDecimal.valueOf(9999))
                .paymentStatus(PaymentStatus.UNPAID)
                .build());

        // Our patient should only see their own bill
        var bills = patientPortalService.getMyBills(patientPrincipal);
        assertThat(bills).allMatch(b -> b.patientId().equals(patient.getId()));
        assertThat(bills).noneMatch(b -> b.totalAmount().compareTo(BigDecimal.valueOf(5000)) > 0);
    }
}

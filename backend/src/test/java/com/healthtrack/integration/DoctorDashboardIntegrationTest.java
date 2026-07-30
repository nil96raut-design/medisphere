package com.healthtrack.integration;

import com.healthtrack.dto.AppointmentDtos.AppointmentRequest;
import com.healthtrack.dto.AppointmentDtos.AppointmentResponse;
import com.healthtrack.dto.AppointmentDtos.StatusUpdateRequest;
import com.healthtrack.dto.AppointmentDtos.DoctorResponse;
import com.healthtrack.dto.DoctorDtos.DoctorStatsResponse;
import com.healthtrack.dto.DoctorDtos.PatientFullProfileResponse;
import com.healthtrack.dto.DoctorDtos.TodayScheduleResponse;
import com.healthtrack.dto.EmrDtos.CreateMedicalRecordRequest;
import com.healthtrack.dto.EmrDtos.PrescriptionItemRequest;
import com.healthtrack.entity.*;
import com.healthtrack.repository.*;
import com.healthtrack.security.UserPrincipal;
import com.healthtrack.service.AppointmentService;
import com.healthtrack.service.DoctorService;
import com.healthtrack.service.MedicalRecordService;
import com.healthtrack.support.PostgresTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DoctorDashboardIntegrationTest extends PostgresTestBase {

    @Autowired private DoctorService doctorService;
    @Autowired private AppointmentService appointmentService;
    @Autowired private MedicalRecordService medicalRecordService;
    @Autowired private HospitalRepository hospitalRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private DoctorRepository doctorRepository;
    @Autowired private AppointmentRepository appointmentRepository;

    private Hospital hospital;
    private User doctorUser;
    private Doctor doctor;
    private Patient patient1;
    private Patient patient2;
    private UserPrincipal doctorPrincipal;
    private UserPrincipal adminPrincipal;
    private Long doctorId;

    @BeforeEach
    void setUp() {
        hospital = hospitalRepository.save(Hospital.builder()
                .name("Doc Dashboard Test Hospital").licenseNumber("DOCD-" + System.nanoTime())
                .contactEmail("docd-test@test.com")
                .subscriptionTier(SubscriptionTier.MONTHLY)
                .subscriptionStatus(SubscriptionStatus.ACTIVE).build());

        doctorUser = userRepository.save(User.builder()
                .fullName("Dr. Dashboard").email("docd-main-" + System.nanoTime() + "@test.com")
                .passwordHash("x").role(Role.DOCTOR).hospital(hospital).build());

        User adminUser = userRepository.save(User.builder()
                .fullName("Admin User").email("admin-docd-" + System.nanoTime() + "@test.com")
                .passwordHash("x").role(Role.ADMIN).hospital(hospital).build());

        doctor = doctorRepository.save(Doctor.builder()
                .hospital(hospital).user(doctorUser)
                .specialization("Cardiology").consultationFee(BigDecimal.valueOf(500))
                .isAvailable(true).build());
        doctorId = doctor.getId();

        patient1 = patientRepository.save(Patient.builder()
                .hospital(hospital).firstName("John").lastName("Doe")
                .phoneNumber("1111111111").gender("Male")
                .dateOfBirth(LocalDate.of(1990, 1, 15)).build());

        patient2 = patientRepository.save(Patient.builder()
                .hospital(hospital).firstName("Jane").lastName("Smith")
                .phoneNumber("2222222222").gender("Female")
                .dateOfBirth(LocalDate.of(1985, 6, 20)).build());

        doctorPrincipal = new UserPrincipal(doctorUser);
        adminPrincipal = new UserPrincipal(adminUser);

        // Create an appointment for today
        AppointmentRequest req = new AppointmentRequest(
                patient1.getId(), doctorId, LocalDate.now(),
                LocalTime.of(10, 0), LocalTime.of(10, 30), false);
        appointmentService.bookAppointment(req, adminPrincipal);
    }

    @Test
    void doctorCanGetTodaySchedule() {
        List<TodayScheduleResponse> schedule = doctorService.getTodaySchedule(doctorPrincipal);
        assertThat(schedule).hasSize(1);
        assertThat(schedule.get(0).patientName()).contains("John");
        assertThat(schedule.get(0).tokenNumber()).isNull(); // not checked in yet
    }

    @Test
    void doctorCanUpdateAppointmentStatus() {
        Appointment appt = appointmentRepository.findByPatientId(patient1.getId()).get(0);

        // Check in
        AppointmentResponse checkedIn = appointmentService.updateStatus(
                appt.getId(), new StatusUpdateRequest(AppointmentStatus.CHECKED_IN), doctorPrincipal);
        assertThat(checkedIn.status()).isEqualTo(AppointmentStatus.CHECKED_IN);
        assertThat(checkedIn.tokenNumber()).isNotNull();

        // Start consultation
        AppointmentResponse consulting = appointmentService.updateStatus(
                appt.getId(), new StatusUpdateRequest(AppointmentStatus.IN_CONSULTATION), doctorPrincipal);
        assertThat(consulting.status()).isEqualTo(AppointmentStatus.IN_CONSULTATION);

        // Complete
        AppointmentResponse completed = appointmentService.updateStatus(
                appt.getId(), new StatusUpdateRequest(AppointmentStatus.COMPLETED), doctorPrincipal);
        assertThat(completed.status()).isEqualTo(AppointmentStatus.COMPLETED);
    }

    @Test
    void doctorCanGetStats() {
        DoctorStatsResponse stats = doctorService.getStats(doctorPrincipal);
        assertThat(stats.patientsToday()).isEqualTo(1);
        assertThat(stats.completedConsultations()).isZero();
        assertThat(stats.pendingConsultations()).isEqualTo(1);
    }

    @Test
    void doctorCanGetPatientFullProfile() {
        PatientFullProfileResponse profile = doctorService.getPatientFullProfile(patient1.getId(), doctorPrincipal);
        assertThat(profile.patient()).isNotNull();
        assertThat(profile.patient().firstName()).isEqualTo("John");
        assertThat(profile.appointments()).hasSize(1);
        assertThat(profile.medicalRecords()).isEmpty(); // no records yet
        assertThat(profile.labOrders()).isEmpty(); // no lab orders yet
    }

    @Test
    void doctorCanCreateMedicalRecordWithPrescriptions() {
        var presc = List.of(new PrescriptionItemRequest("Amoxicillin", "500mg", "TID", "7 days", "After meals"));
        CreateMedicalRecordRequest recordReq = new CreateMedicalRecordRequest(
                patient1.getId(), null, LocalDate.now(),
                "Chest pain and shortness of breath",
                "BP 130/85, HR 78", "Hypertension",
                LocalDate.now().plusWeeks(2), presc, null);

        var record = medicalRecordService.createRecord(recordReq, doctorPrincipal);
        assertThat(record).isNotNull();
        assertThat(record.patientId()).isEqualTo(patient1.getId());
        assertThat(record.prescriptions()).hasSize(1);
        assertThat(record.prescriptions().get(0).medicineName()).isEqualTo("Amoxicillin");

        // Verify profile now includes the record
        PatientFullProfileResponse profile = doctorService.getPatientFullProfile(patient1.getId(), doctorPrincipal);
        assertThat(profile.medicalRecords()).hasSize(1);
    }

    @Test
    void adminWithoutDoctorProfileCannotGetSchedule() {
        // Admin with no doctor profile cannot get schedule (expects 404 from Doctor entity lookup)
        assertThatThrownBy(() -> doctorService.getTodaySchedule(adminPrincipal))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Doctor profile not found");
    }

    @Test
    void doctorOnlySeesOwnHospitalPatients() {
        // Create another hospital's patient
        Hospital otherHospital = hospitalRepository.save(Hospital.builder()
                .name("Other Hospital").licenseNumber("OTHER-" + System.nanoTime())
                .contactEmail("other@test.com")
                .subscriptionTier(SubscriptionTier.MONTHLY)
                .subscriptionStatus(SubscriptionStatus.ACTIVE).build());

        Patient otherPatient = patientRepository.save(Patient.builder()
                .hospital(otherHospital).firstName("Other").lastName("Patient")
                .phoneNumber("9999999999").build());

        // Doctor should not be able to access other hospital's patient
        assertThatThrownBy(() -> doctorService.getPatientFullProfile(otherPatient.getId(), doctorPrincipal))
                .isInstanceOf(ResponseStatusException.class);
    }
}

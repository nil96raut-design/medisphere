package com.healthtrack.service;

import com.healthtrack.dto.AppointmentDtos.*;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppointmentServiceTest extends PostgresTestBase {

    @Autowired private AppointmentService appointmentService;
    @Autowired private HospitalRepository hospitalRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private DoctorRepository doctorRepository;

    private Hospital hospital;
    private Patient patient1;
    private Patient patient2;
    private Doctor doctor;
    private UserPrincipal staffPrincipal;
    private Long doctorId;

    @BeforeEach
    void setUp() {
        hospital = hospitalRepository.save(Hospital.builder()
                .name("Appt Test Hospital").licenseNumber("APPT-" + System.nanoTime())
                .contactEmail("appt@test.com")
                .subscriptionTier(SubscriptionTier.MONTHLY)
                .subscriptionStatus(SubscriptionStatus.ACTIVE).build());

        User staffUser = userRepository.save(User.builder()
                .fullName("Staff User").email("staff-appt-" + System.nanoTime() + "@test.com")
                .passwordHash("x").role(Role.RECEPTIONIST).hospital(hospital).build());

        User docUser = userRepository.save(User.builder()
                .fullName("Dr. Busy").email("doc-appt-" + System.nanoTime() + "@test.com")
                .passwordHash("x").role(Role.DOCTOR).hospital(hospital).build());

        staffPrincipal = new UserPrincipal(staffUser);

        patient1 = patientRepository.save(Patient.builder()
                .hospital(hospital).firstName("Alice").lastName("Appt")
                .phoneNumber("555-APT-1").build());
        patient2 = patientRepository.save(Patient.builder()
                .hospital(hospital).firstName("Bob").lastName("Appt")
                .phoneNumber("555-APT-2").build());

        doctor = doctorRepository.save(Doctor.builder()
                .hospital(hospital).user(docUser)
                .specialization("General")
                .consultationFee(new BigDecimal("150"))
                .isAvailable(true).build());
        doctorId = doctor.getId();
    }

    @Test
    void bookAppointment_createsAppointment() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        AppointmentResponse response = appointmentService.bookAppointment(
                new AppointmentRequest(patient1.getId(), doctorId, tomorrow, LocalTime.of(9, 0), LocalTime.of(9, 30)),
                staffPrincipal);

        assertThat(response.patientId()).isEqualTo(patient1.getId());
        assertThat(response.doctorId()).isEqualTo(doctorId);
        assertThat(response.status()).isEqualTo(AppointmentStatus.SCHEDULED);
    }

    @Test
    void bookAppointment_overlappingSlot_throws409() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalTime start = LocalTime.of(10, 0);
        LocalTime end = LocalTime.of(10, 30);

        appointmentService.bookAppointment(
                new AppointmentRequest(patient1.getId(), doctorId, tomorrow, start, end),
                staffPrincipal);

        assertThatThrownBy(() -> appointmentService.bookAppointment(
                new AppointmentRequest(patient2.getId(), doctorId, tomorrow, start, end),
                staffPrincipal))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409 CONFLICT");
    }

    @Test
    void updateStatus_checkIn_assignsToken() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        AppointmentResponse appointment = appointmentService.bookAppointment(
                new AppointmentRequest(patient1.getId(), doctorId, tomorrow, LocalTime.of(11, 0), LocalTime.of(11, 30)),
                staffPrincipal);

        AppointmentResponse checkedIn = appointmentService.updateStatus(
                appointment.id(), new StatusUpdateRequest(AppointmentStatus.CHECKED_IN), staffPrincipal);

        assertThat(checkedIn.tokenNumber()).isNotNull();
        assertThat(checkedIn.status()).isEqualTo(AppointmentStatus.CHECKED_IN);
    }

    @Test
    void updateStatus_completingScheduled_isAllowed() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        AppointmentResponse appointment = appointmentService.bookAppointment(
                new AppointmentRequest(patient1.getId(), doctorId, tomorrow, LocalTime.of(14, 0), LocalTime.of(14, 30)),
                staffPrincipal);

        AppointmentResponse updated = appointmentService.updateStatus(
                appointment.id(), new StatusUpdateRequest(AppointmentStatus.COMPLETED), staffPrincipal);

        assertThat(updated.status()).isEqualTo(AppointmentStatus.COMPLETED);
    }

    @Test
    void updateStatus_checkingInCompleted_throws400() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        AppointmentResponse appointment = appointmentService.bookAppointment(
                new AppointmentRequest(patient1.getId(), doctorId, tomorrow, LocalTime.of(14, 0), LocalTime.of(14, 30)),
                staffPrincipal);

        appointmentService.updateStatus(
                appointment.id(), new StatusUpdateRequest(AppointmentStatus.COMPLETED), staffPrincipal);

        assertThatThrownBy(() -> appointmentService.updateStatus(
                appointment.id(), new StatusUpdateRequest(AppointmentStatus.CHECKED_IN), staffPrincipal))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Only SCHEDULED");
    }

    @Test
    void getQueue_returnsAllNonCancelledAppointments() {
        LocalDate today = LocalDate.now();
        AppointmentResponse a1 = appointmentService.bookAppointment(
                new AppointmentRequest(patient1.getId(), doctorId, today, LocalTime.of(9, 0), LocalTime.of(9, 30)),
                staffPrincipal);
        appointmentService.bookAppointment(
                new AppointmentRequest(patient2.getId(), doctorId, today, LocalTime.of(9, 30), LocalTime.of(10, 0)),
                staffPrincipal);

        appointmentService.updateStatus(a1.id(), new StatusUpdateRequest(AppointmentStatus.CHECKED_IN), staffPrincipal);

        var queue = appointmentService.getQueue(doctorId, staffPrincipal);
        assertThat(queue).hasSize(2);
        var checkedIn = queue.stream().filter(q -> q.status() == AppointmentStatus.CHECKED_IN).findFirst().orElseThrow();
        assertThat(checkedIn.tokenNumber()).isEqualTo(1);
    }
}

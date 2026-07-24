package com.healthtrack.service;

import com.healthtrack.dto.AppointmentDtos.AppointmentRequest;
import com.healthtrack.dto.AppointmentDtos.AppointmentResponse;
import com.healthtrack.entity.*;
import com.healthtrack.repository.*;
import com.healthtrack.security.UserPrincipal;
import com.healthtrack.support.PostgresTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AppointmentConcurrencyTest extends PostgresTestBase {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    /**
     * Two genuiney concurrent booking requests for the same doctor + same
     * open slot (no prior Appointment exists). The Doctor-row FOR UPDATE
     * lock must serialize them: one commits, the other sees the overlap
     * and gets 409. Repeated 20 times to verify the race is always caught.
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentBooking_sameDoctorSameSlot_oneSucceeds() throws Exception {
        var ctx = setupTestData();
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            for (int i = 0; i < 20; i++) {
                LocalDate date = LocalDate.now().plusDays(i + 1);
                LocalTime start = LocalTime.of(9, 0);
                LocalTime end = LocalTime.of(9, 30);

                CountDownLatch barrier = new CountDownLatch(1);

                Future<AppointmentResponse> f1 = executor.submit(() -> {
                    barrier.await();
                    return appointmentService.bookAppointment(
                            new AppointmentRequest(ctx.patient1Id, ctx.doctor1Id, date, start, end),
                            ctx.staffPrincipal);
                });

                Future<AppointmentResponse> f2 = executor.submit(() -> {
                    barrier.await();
                    return appointmentService.bookAppointment(
                            new AppointmentRequest(ctx.patient2Id, ctx.doctor1Id, date, start, end),
                            ctx.staffPrincipal);
                });

                // Give both threads time to reach the barrier
                Thread.sleep(50);
                barrier.countDown();

                int successCount = 0;
                int conflictCount = 0;

                // Check first future
                try {
                    AppointmentResponse r = f1.get(10, TimeUnit.SECONDS);
                    assertThat(r).isNotNull();
                    successCount++;
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    assertThat(cause).isInstanceOf(ResponseStatusException.class);
                    assertThat(((ResponseStatusException) cause).getStatusCode().value())
                            .isEqualTo(HttpStatus.CONFLICT.value());
                    conflictCount++;
                }

                // Check second future
                try {
                    AppointmentResponse r = f2.get(10, TimeUnit.SECONDS);
                    assertThat(r).isNotNull();
                    successCount++;
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    assertThat(cause).isInstanceOf(ResponseStatusException.class);
                    assertThat(((ResponseStatusException) cause).getStatusCode().value())
                            .isEqualTo(HttpStatus.CONFLICT.value());
                    conflictCount++;
                }

                assertThat(successCount)
                        .as("Run " + i + ": exactly one booking should succeed")
                        .isEqualTo(1);
                assertThat(conflictCount)
                        .as("Run " + i + ": exactly one should be rejected with 409")
                        .isEqualTo(1);
            }
        } finally {
            executor.shutdown();
        }
    }

    /**
     * Two concurrent bookings for DIFFERENT doctors at the same time slot.
     * Each acquires a FOR UPDATE lock on its own Doctor row, so neither
     * blocks on the other. Both should succeed.
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentBooking_differentDoctors_bothSucceed() throws Exception {
        var ctx = setupTestData();
        LocalDate date = LocalDate.now().plusDays(100);
        LocalTime start = LocalTime.of(10, 0);
        LocalTime end = LocalTime.of(10, 30);

        CountDownLatch barrier = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Future<AppointmentResponse> f1 = executor.submit(() -> {
            barrier.await();
            return appointmentService.bookAppointment(
                    new AppointmentRequest(ctx.patient1Id, ctx.doctor1Id, date, start, end),
                    ctx.staffPrincipal);
        });

        Future<AppointmentResponse> f2 = executor.submit(() -> {
            barrier.await();
            return appointmentService.bookAppointment(
                    new AppointmentRequest(ctx.patient2Id, ctx.doctor2Id, date, start, end),
                    ctx.staffPrincipal);
        });

        Thread.sleep(50);
        barrier.countDown();

        AppointmentResponse r1 = f1.get(10, TimeUnit.SECONDS);
        AppointmentResponse r2 = f2.get(10, TimeUnit.SECONDS);

        assertThat(r1).isNotNull();
        assertThat(r2).isNotNull();
        assertThat(r1.doctorId()).isEqualTo(ctx.doctor1Id);
        assertThat(r2.doctorId()).isEqualTo(ctx.doctor2Id);

        executor.shutdown();
    }

    private TestContext setupTestData() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> {
            Hospital hospital = hospitalRepository.save(Hospital.builder()
                    .name("Test Hospital")
                    .licenseNumber("TEST-" + System.nanoTime())
                    .contactEmail("test@hospital.com")
                    .subscriptionTier(SubscriptionTier.MONTHLY)
                    .subscriptionStatus(SubscriptionStatus.ACTIVE)
                    .build());

            Patient patient1 = patientRepository.save(Patient.builder()
                    .hospital(hospital).firstName("Alice").lastName("Patient")
                    .phoneNumber("111-111-1111").build());

            Patient patient2 = patientRepository.save(Patient.builder()
                    .hospital(hospital).firstName("Bob").lastName("Patient")
                    .phoneNumber("222-222-2222").build());

            User staffUser = userRepository.save(User.builder()
                    .fullName("Staff User")
                    .email("staff-" + System.nanoTime() + "@test.com")
                    .passwordHash("x")
                    .role(Role.RECEPTIONIST)
                    .hospital(hospital)
                    .build());

            User docUser1 = userRepository.save(User.builder()
                    .fullName("Dr. Cardio")
                    .email("doc1-" + System.nanoTime() + "@test.com")
                    .passwordHash("x")
                    .role(Role.DOCTOR)
                    .hospital(hospital)
                    .build());

            User docUser2 = userRepository.save(User.builder()
                    .fullName("Dr. Derma")
                    .email("doc2-" + System.nanoTime() + "@test.com")
                    .passwordHash("x")
                    .role(Role.DOCTOR)
                    .hospital(hospital)
                    .build());

            Doctor doctor1 = doctorRepository.save(Doctor.builder()
                    .hospital(hospital).user(docUser1)
                    .specialization("Cardiology")
                    .consultationFee(new BigDecimal("100"))
                    .isAvailable(true)
                    .build());

            Doctor doctor2 = doctorRepository.save(Doctor.builder()
                    .hospital(hospital).user(docUser2)
                    .specialization("Dermatology")
                    .consultationFee(new BigDecimal("80"))
                    .isAvailable(true)
                    .build());

            return new TestContext(
                    patient1.getId(), patient2.getId(),
                    doctor1.getId(), doctor2.getId(),
                    new UserPrincipal(staffUser));
        });
    }

    private record TestContext(
            Long patient1Id, Long patient2Id,
            Long doctor1Id, Long doctor2Id,
            UserPrincipal staffPrincipal) {}
}

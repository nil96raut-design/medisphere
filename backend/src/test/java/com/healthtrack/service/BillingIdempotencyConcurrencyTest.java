package com.healthtrack.service;

import com.healthtrack.dto.BillingDtos.SettleRequest;
import com.healthtrack.dto.BillingDtos.BillResponse;
import com.healthtrack.entity.*;
import com.healthtrack.repository.*;
import com.healthtrack.security.UserPrincipal;
import com.healthtrack.support.PostgresTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BillingIdempotencyConcurrencyTest extends PostgresTestBase {

    @Autowired private BillingService billingService;
    @Autowired private PatientRepository patientRepository;
    @Autowired private HospitalRepository hospitalRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private DoctorRepository doctorRepository;
    @Autowired private AppointmentRepository appointmentRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    /**
     * Two concurrent settlement attempts with the same idempotency key.
     * The idempotency_key UNIQUE constraint + service-level check ensure
     * exactly one succeeds; the other gets 409 CONFLICT.
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentSettlement_sameKey_oneSucceeds() throws Exception {
        var ctx = setupTestData();
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            for (int i = 0; i < 10; i++) {
                String idempotencyKey = UUID.randomUUID().toString();

                CountDownLatch barrier = new CountDownLatch(1);

                Future<BillResponse> f1 = executor.submit(() -> {
                    barrier.await();
                    return billingService.settle(
                            new SettleRequest(ctx.patientId, BigDecimal.ZERO, BigDecimal.ZERO, "CASH", idempotencyKey),
                            ctx.staffPrincipal);
                });

                Future<BillResponse> f2 = executor.submit(() -> {
                    barrier.await();
                    return billingService.settle(
                            new SettleRequest(ctx.patientId, BigDecimal.ZERO, BigDecimal.ZERO, "CASH", idempotencyKey),
                            ctx.staffPrincipal);
                });

                Thread.sleep(50);
                barrier.countDown();

                int successCount = 0;
                int conflictCount = 0;

                for (Future<BillResponse> f : new Future[]{f1, f2}) {
                    try {
                        BillResponse r = f.get(10, TimeUnit.SECONDS);
                        assertThat(r).isNotNull();
                        successCount++;
                    } catch (ExecutionException e) {
                        Throwable cause = e.getCause();
                        if (cause instanceof ResponseStatusException rse) {
                            assertThat(rse.getStatusCode().value())
                                    .isEqualTo(HttpStatus.CONFLICT.value());
                        } else {
                            assertThat(cause).isInstanceOf(DataIntegrityViolationException.class);
                        }
                        conflictCount++;
                    }
                }

                assertThat(successCount)
                        .as("Run " + i + ": exactly one settlement should succeed")
                        .isEqualTo(1);
                assertThat(conflictCount)
                        .as("Run " + i + ": exactly one should be rejected with 409")
                        .isEqualTo(1);
            }
        } finally {
            executor.shutdown();
        }
    }

    private TestContext setupTestData() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> {
            Hospital hospital = hospitalRepository.save(Hospital.builder()
                    .name("Billing Concurrency Hospital").licenseNumber("BILLCON-" + System.nanoTime())
                    .contactEmail("billcon@test.com")
                    .subscriptionTier(SubscriptionTier.MONTHLY)
                    .subscriptionStatus(SubscriptionStatus.ACTIVE).build());

            User staffUser = userRepository.save(User.builder()
                    .fullName("Bill Staff").email("staff-billcon-" + System.nanoTime() + "@test.com")
                    .passwordHash("x").role(Role.RECEPTIONIST).hospital(hospital).build());

            User docUser = userRepository.save(User.builder()
                    .fullName("Dr. BillCon").email("doc-billcon-" + System.nanoTime() + "@test.com")
                    .passwordHash("x").role(Role.DOCTOR).hospital(hospital).build());

            Patient patient = patientRepository.save(Patient.builder()
                    .hospital(hospital).firstName("Concurrent").lastName("Billing")
                    .phoneNumber("555-BILLCON-" + System.nanoTime()).build());

            Doctor doctor = doctorRepository.save(Doctor.builder()
                    .hospital(hospital).user(docUser)
                    .specialization("General")
                    .consultationFee(new BigDecimal("200"))
                    .isAvailable(true).build());

            appointmentRepository.save(Appointment.builder()
                    .hospital(hospital).patient(patient).doctor(doctor)
                    .appointmentDate(LocalDate.now())
                    .startTime(java.time.LocalTime.of(9, 0))
                    .endTime(java.time.LocalTime.of(9, 30))
                    .status(AppointmentStatus.COMPLETED).build());

            return new TestContext(patient.getId(), new UserPrincipal(staffUser));
        });
    }

    private record TestContext(Long patientId, UserPrincipal staffPrincipal) {}
}

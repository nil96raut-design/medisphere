package com.healthtrack.service;

import com.healthtrack.dto.IpdDtos.AdmissionRequest;
import com.healthtrack.dto.IpdDtos.AdmissionResponse;
import com.healthtrack.entity.*;
import com.healthtrack.repository.*;
import com.healthtrack.security.UserPrincipal;
import com.healthtrack.support.PostgresTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BedAllocationConcurrencyTest extends PostgresTestBase {

    @Autowired private IpdService ipdService;
    @Autowired private PatientRepository patientRepository;
    @Autowired private BedRepository bedRepository;
    @Autowired private HospitalRepository hospitalRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private DoctorRepository doctorRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    /**
     * Two concurrent admission requests for the same bed.
     * The PESSIMISTIC_WRITE lock on the Bed row serializes them:
     * one succeeds, the other gets 409 CONFLICT.
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentBedAllocation_sameBed_oneSucceeds() throws Exception {
        var ctx = setupTestData();
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            for (int idx = 0; idx < 10; idx++) {
                int i = idx;
                LocalDate date = LocalDate.now().plusDays(i + 1);

                Bed freshBed = new TransactionTemplate(transactionManager).execute(status ->
                    bedRepository.save(Bed.builder()
                            .hospital(ctx.hospital).wardName("Concurrency Ward")
                            .bedNumber("C-" + i).chargePerDay(new BigDecimal("500"))
                            .isOccupied(false).build())
                );

                CountDownLatch barrier = new CountDownLatch(1);

                Future<AdmissionResponse> f1 = executor.submit(() -> {
                    barrier.await();
                    return ipdService.admitPatient(
                            new AdmissionRequest(ctx.patient1Id, ctx.doctorId, freshBed.getId(), date, "Race 1"),
                            ctx.doctorPrincipal);
                });

                Future<AdmissionResponse> f2 = executor.submit(() -> {
                    barrier.await();
                    return ipdService.admitPatient(
                            new AdmissionRequest(ctx.patient2Id, ctx.doctorId, freshBed.getId(), date, "Race 2"),
                            ctx.doctorPrincipal);
                });

                Thread.sleep(50);
                barrier.countDown();

                int successCount = 0;
                int conflictCount = 0;

                for (Future<AdmissionResponse> f : new Future[]{f1, f2}) {
                    try {
                        AdmissionResponse r = f.get(10, TimeUnit.SECONDS);
                        assertThat(r).isNotNull();
                        successCount++;
                    } catch (ExecutionException e) {
                        Throwable cause = e.getCause();
                        assertThat(cause).isInstanceOf(ResponseStatusException.class);
                        assertThat(((ResponseStatusException) cause).getStatusCode().value())
                                .isEqualTo(HttpStatus.CONFLICT.value());
                        conflictCount++;
                    }
                }

                assertThat(successCount)
                        .as("Run " + i + ": exactly one admission should succeed")
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
     * Two concurrent admissions for DIFFERENT beds.
     * Each locks its own Bed row, so both should succeed.
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentBedAllocation_differentBeds_bothSucceed() throws Exception {
        var ctx = setupTestData();
        LocalDate date = LocalDate.now().plusDays(100);

        Bed bed1 = new TransactionTemplate(transactionManager).execute(status ->
            bedRepository.save(Bed.builder()
                    .hospital(ctx.hospital).wardName("Ward A")
                    .bedNumber("A-1").chargePerDay(new BigDecimal("300"))
                    .isOccupied(false).build())
        );

        Bed bed2 = new TransactionTemplate(transactionManager).execute(status ->
            bedRepository.save(Bed.builder()
                    .hospital(ctx.hospital).wardName("Ward B")
                    .bedNumber("B-1").chargePerDay(new BigDecimal("400"))
                    .isOccupied(false).build())
        );

        CountDownLatch barrier = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Future<AdmissionResponse> f1 = executor.submit(() -> {
            barrier.await();
            return ipdService.admitPatient(
                    new AdmissionRequest(ctx.patient1Id, ctx.doctorId, bed1.getId(), date, "Bed 1"),
                    ctx.doctorPrincipal);
        });

        Future<AdmissionResponse> f2 = executor.submit(() -> {
            barrier.await();
            return ipdService.admitPatient(
                    new AdmissionRequest(ctx.patient2Id, ctx.doctorId, bed2.getId(), date, "Bed 2"),
                    ctx.doctorPrincipal);
        });

        Thread.sleep(50);
        barrier.countDown();

        AdmissionResponse r1 = f1.get(10, TimeUnit.SECONDS);
        AdmissionResponse r2 = f2.get(10, TimeUnit.SECONDS);

        assertThat(r1).isNotNull();
        assertThat(r2).isNotNull();
        assertThat(r1.bedId()).isEqualTo(bed1.getId());
        assertThat(r2.bedId()).isEqualTo(bed2.getId());

        executor.shutdown();
    }

    private TestContext setupTestData() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> {
            Hospital hospital = hospitalRepository.save(Hospital.builder()
                    .name("Bed Concurrency Hospital").licenseNumber("BEDCON-" + System.nanoTime())
                    .contactEmail("bedcon@test.com")
                    .subscriptionTier(SubscriptionTier.MONTHLY)
                    .subscriptionStatus(SubscriptionStatus.ACTIVE).build());

            Patient patient1 = patientRepository.save(Patient.builder()
                    .hospital(hospital).firstName("Alice").lastName("Concurrent")
                    .phoneNumber("555-BED-1").build());

            Patient patient2 = patientRepository.save(Patient.builder()
                    .hospital(hospital).firstName("Bob").lastName("Concurrent")
                    .phoneNumber("555-BED-2").build());

            User docUser = userRepository.save(User.builder()
                    .fullName("Dr. Concurrent").email("doc-con-" + System.nanoTime() + "@test.com")
                    .passwordHash("x").role(Role.DOCTOR).hospital(hospital).build());

            Doctor doctor = doctorRepository.save(Doctor.builder()
                    .hospital(hospital).user(docUser)
                    .specialization("General")
                    .consultationFee(new BigDecimal("100"))
                    .isAvailable(true).build());

            return new TestContext(
                    hospital, patient1.getId(), patient2.getId(),
                    docUser.getId(), new UserPrincipal(docUser));
        });
    }

    private record TestContext(
            Hospital hospital,
            Long patient1Id, Long patient2Id,
            Long doctorId,
            UserPrincipal doctorPrincipal) {}
}

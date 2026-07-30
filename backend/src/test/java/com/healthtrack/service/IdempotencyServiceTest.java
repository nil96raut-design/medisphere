package com.healthtrack.service;

import com.healthtrack.entity.*;
import com.healthtrack.repository.*;
import com.healthtrack.support.PostgresTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class IdempotencyServiceTest extends PostgresTestBase {

    @Autowired private IdempotencyService idempotencyService;
    @Autowired private HospitalRepository hospitalRepository;

    private Long hospitalId;

    @BeforeEach
    void setUp() {
        Hospital hospital = hospitalRepository.save(Hospital.builder()
                .name("Idempotency Hospital").licenseNumber("ID-" + System.nanoTime())
                .contactEmail("id@test.com").subscriptionTier(SubscriptionTier.MONTHLY)
                .subscriptionStatus(SubscriptionStatus.ACTIVE).build());
        hospitalId = hospital.getId();
    }

    @Test
    void tryProcess_firstCallReturnsTrue() {
        assertThat(idempotencyService.tryProcess("req-1", hospitalId, "TEST_ACTION")).isTrue();
    }

    @Test
    void tryProcess_duplicateReturnsFalse() {
        idempotencyService.tryProcess("req-2", hospitalId, "TEST_ACTION");
        assertThat(idempotencyService.tryProcess("req-2", hospitalId, "TEST_ACTION")).isFalse();
    }

    @Test
    void isProcessed_returnsTrueForProcessed() {
        idempotencyService.markProcessed("req-3", hospitalId, "TEST_ACTION", "done");
        assertThat(idempotencyService.isProcessed("req-3")).isTrue();
    }

    @Test
    void isProcessed_returnsFalseForUnknown() {
        assertThat(idempotencyService.isProcessed("req-unknown")).isFalse();
    }
}

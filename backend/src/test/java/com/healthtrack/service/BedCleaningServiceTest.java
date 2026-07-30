package com.healthtrack.service;

import com.healthtrack.entity.*;
import com.healthtrack.repository.*;
import com.healthtrack.security.UserPrincipal;
import com.healthtrack.support.PostgresTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BedCleaningServiceTest extends PostgresTestBase {

    @Autowired private BedCleaningService bedCleaningService;
    @Autowired private BedCleaningRepository bedCleaningRepository;
    @Autowired private BedRepository bedRepository;
    @Autowired private HospitalRepository hospitalRepository;
    @Autowired private UserRepository userRepository;

    private Hospital hospital;
    private Bed bed;
    private User nurse;
    private UserPrincipal nursePrincipal;

    @BeforeEach
    void setUp() {
        hospital = hospitalRepository.save(Hospital.builder()
                .name("BedClean Hospital").licenseNumber("BC-" + System.nanoTime())
                .contactEmail("bc@test.com").subscriptionTier(SubscriptionTier.MONTHLY)
                .subscriptionStatus(SubscriptionStatus.ACTIVE).build());

        bed = bedRepository.save(Bed.builder()
                .hospital(hospital).wardName("Test Ward").bedNumber("BC-101")
                .chargePerDay(new BigDecimal("50")).isOccupied(true).build());

        nurse = userRepository.save(User.builder()
                .fullName("Nurse Clean").email("nurse-bc-" + System.nanoTime() + "@test.com")
                .passwordHash("x").role(Role.NURSE).hospital(hospital).build());

        nursePrincipal = new UserPrincipal(nurse);
    }

    @Test
    void requestCleaning_createsRequest() {
        var response = bedCleaningService.requestCleaning(bed.getId(), nursePrincipal);
        assertThat(response.status()).isEqualTo(CleaningStatus.REQUESTED);
        assertThat(response.bedId()).isEqualTo(bed.getId());
    }

    @Test
    void markCleaned_changesStatus() {
        var created = bedCleaningService.requestCleaning(bed.getId(), nursePrincipal);
        var cleaned = bedCleaningService.markCleaned(created.id(), nursePrincipal);
        assertThat(cleaned.status()).isEqualTo(CleaningStatus.CLEANED);
    }

    @Test
    void duplicateRequest_throws() {
        bedCleaningService.requestCleaning(bed.getId(), nursePrincipal);
        assertThatThrownBy(() -> bedCleaningService.requestCleaning(bed.getId(), nursePrincipal))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already requested");
    }

    @Test
    void bedBlockedUntilCleaned() {
        bedCleaningService.requestCleaning(bed.getId(), nursePrincipal);
        assertThat(bedRepository.findById(bed.getId()).orElseThrow().getIsOccupied()).isTrue();

        var requests = bedCleaningService.getPendingCleaningRequests(nursePrincipal);
        assertThat(requests).isNotEmpty();
    }

    @Test
    void nonOccupiedBed_throws() {
        bed.setIsOccupied(false);
        bedRepository.save(bed);
        assertThatThrownBy(() -> bedCleaningService.requestCleaning(bed.getId(), nursePrincipal))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not occupied");
    }
}

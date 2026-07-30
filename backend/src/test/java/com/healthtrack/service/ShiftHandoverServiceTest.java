package com.healthtrack.service;

import com.healthtrack.dto.ClinicalSafetyDtos.ShiftHandoverRequest;
import com.healthtrack.entity.*;
import com.healthtrack.repository.*;
import com.healthtrack.security.UserPrincipal;
import com.healthtrack.support.PostgresTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShiftHandoverServiceTest extends PostgresTestBase {

    @Autowired private ShiftHandoverService shiftHandoverService;
    @Autowired private HospitalRepository hospitalRepository;
    @Autowired private UserRepository userRepository;

    private Hospital hospital;
    private User fromNurse;
    private User toNurse;
    private UserPrincipal fromPrincipal;

    @BeforeEach
    void setUp() {
        hospital = hospitalRepository.save(Hospital.builder()
                .name("Handover Hospital").licenseNumber("HO-" + System.nanoTime())
                .contactEmail("ho@test.com").subscriptionTier(SubscriptionTier.MONTHLY)
                .subscriptionStatus(SubscriptionStatus.ACTIVE).build());

        fromNurse = userRepository.save(User.builder()
                .fullName("Nurse From").email("from-" + System.nanoTime() + "@test.com")
                .passwordHash("x").role(Role.NURSE).hospital(hospital).build());

        toNurse = userRepository.save(User.builder()
                .fullName("Nurse To").email("to-" + System.nanoTime() + "@test.com")
                .passwordHash("x").role(Role.NURSE).hospital(hospital).build());

        fromPrincipal = new UserPrincipal(fromNurse);
    }

    @Test
    void submitHandover_createsHandover() {
        var request = new ShiftHandoverRequest(toNurse.getId(), "General Ward",
                "Handing over all patients", "3 patients stable");
        var response = shiftHandoverService.submitHandover(request, fromPrincipal);
        assertThat(response.fromNurseId()).isEqualTo(fromNurse.getId());
        assertThat(response.toNurseId()).isEqualTo(toNurse.getId());
    }

    @Test
    void submitHandover_toNonNurse_throws() {
        User doctor = userRepository.save(User.builder()
                .fullName("Dr. Wrong").email("wrong-" + System.nanoTime() + "@test.com")
                .passwordHash("x").role(Role.DOCTOR).hospital(hospital).build());

        var request = new ShiftHandoverRequest(doctor.getId(), "ICU", "test", "test");
        assertThatThrownBy(() -> shiftHandoverService.submitHandover(request, fromPrincipal))
                .isInstanceOf(ResponseStatusException.class);
    }
}

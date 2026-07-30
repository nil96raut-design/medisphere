package com.healthtrack.service;

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

class AlertServiceTest extends PostgresTestBase {

    @Autowired private AlertService alertService;
    @Autowired private AlertRepository alertRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private HospitalRepository hospitalRepository;
    @Autowired private UserRepository userRepository;

    private Hospital hospital;
    private Patient patient;
    private User nurse;
    private User doctor;
    private UserPrincipal nursePrincipal;
    private UserPrincipal doctorPrincipal;

    @BeforeEach
    void setUp() {
        hospital = hospitalRepository.save(Hospital.builder()
                .name("Alert Hospital").licenseNumber("AL-" + System.nanoTime())
                .contactEmail("al@test.com").subscriptionTier(SubscriptionTier.MONTHLY)
                .subscriptionStatus(SubscriptionStatus.ACTIVE).build());

        patient = patientRepository.save(Patient.builder()
                .hospital(hospital).firstName("Alert").lastName("Test")
                .phoneNumber("AL-TEST").build());

        nurse = userRepository.save(User.builder()
                .fullName("Nurse Alert").email("nurse-al-" + System.nanoTime() + "@test.com")
                .passwordHash("x").role(Role.NURSE).hospital(hospital).build());

        doctor = userRepository.save(User.builder()
                .fullName("Dr. Alert").email("doc-al-" + System.nanoTime() + "@test.com")
                .passwordHash("x").role(Role.DOCTOR).hospital(hospital).build());

        nursePrincipal = new UserPrincipal(nurse);
        doctorPrincipal = new UserPrincipal(doctor);
    }

    @Test
    void createAlert_createsActiveAlert() {
        var response = alertService.createAlert(hospital.getId(), patient.getId(),
                AlertType.VITAL, AlertSeverity.CRITICAL, "Critical vitals");
        assertThat(response.type()).isEqualTo(AlertType.VITAL);
        assertThat(response.status()).isEqualTo(AlertStatus.ACTIVE);
    }

    @Test
    void acknowledgeAlert_changesStatus() {
        var created = alertService.createAlert(hospital.getId(), patient.getId(),
                AlertType.MEDICATION, AlertSeverity.HIGH, "Missed dose");
        var acked = alertService.acknowledgeAlert(created.id(), nursePrincipal);
        assertThat(acked.status()).isEqualTo(AlertStatus.ACKNOWLEDGED);
    }

    @Test
    void resolveAlert_changesStatus() {
        var created = alertService.createAlert(hospital.getId(), patient.getId(),
                AlertType.LAB, AlertSeverity.LOW, "Lab result abnormal");
        var resolved = alertService.resolveAlert(created.id(), doctorPrincipal);
        assertThat(resolved.status()).isEqualTo(AlertStatus.RESOLVED);
    }

    @Test
    void acknowledgeAlreadyResolved_throws() {
        var created = alertService.createAlert(hospital.getId(), patient.getId(),
                AlertType.VITAL, AlertSeverity.HIGH, "Test");
        alertService.resolveAlert(created.id(), doctorPrincipal);
        assertThatThrownBy(() -> alertService.acknowledgeAlert(created.id(), nursePrincipal))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already resolved");
    }
}

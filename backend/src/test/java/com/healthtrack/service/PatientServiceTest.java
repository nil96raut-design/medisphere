package com.healthtrack.service;

import com.healthtrack.dto.PatientDtos.PatientRegistrationRequest;
import com.healthtrack.dto.PatientDtos.PatientResponse;
import com.healthtrack.entity.*;
import com.healthtrack.repository.HospitalRepository;
import com.healthtrack.repository.PatientRepository;
import com.healthtrack.repository.UserRepository;
import com.healthtrack.security.UserPrincipal;
import com.healthtrack.support.PostgresTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PatientServiceTest extends PostgresTestBase {

    @Autowired
    private PatientService patientService;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private UserRepository userRepository;

    private Hospital hospital;
    private UserPrincipal receptionistPrincipal;
    private UserPrincipal patientPrincipal;

    @BeforeEach
    void setUp() {
        hospital = hospitalRepository.save(Hospital.builder()
                .name("Test Hospital")
                .licenseNumber("LIC-" + System.nanoTime())
                .contactEmail("test@hospital.com")
                .subscriptionTier(SubscriptionTier.MONTHLY)
                .subscriptionStatus(SubscriptionStatus.ACTIVE)
                .build());

        User receptionist = userRepository.save(User.builder()
                .fullName("Receptionist One").email("recep-" + System.nanoTime() + "@test.com")
                .passwordHash("x").role(Role.RECEPTIONIST).hospital(hospital).build());

        User patientUser = userRepository.save(User.builder()
                .fullName("Patient One").email("pat-" + System.nanoTime() + "@test.com")
                .passwordHash("x").role(Role.PATIENT).hospital(hospital).build());

        receptionistPrincipal = new UserPrincipal(receptionist);
        patientPrincipal = new UserPrincipal(patientUser);
    }

    @Test
    void registerPatient_createsPatient() {
        PatientResponse response = patientService.registerPatient(
                new PatientRegistrationRequest("John", "Doe", "Male", null, "555-0100", "john@test.com", null, null, null),
                receptionistPrincipal);

        assertThat(response.firstName()).isEqualTo("John");
        assertThat(response.lastName()).isEqualTo("Doe");
        assertThat(response.phoneNumber()).isEqualTo("555-0100");
    }

    @Test
    void registerPatient_duplicatePhone_throws409() {
        patientService.registerPatient(
                new PatientRegistrationRequest("First", "Patient", null, null, "555-0199", null, null, null, null),
                receptionistPrincipal);

        assertThatThrownBy(() -> patientService.registerPatient(
                new PatientRegistrationRequest("Second", "Patient", null, null, "555-0199", null, null, null, null),
                receptionistPrincipal))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409 CONFLICT");
    }

    @Test
    void registerPatient_nonReceptionist_throws403() {
        assertThatThrownBy(() -> patientService.registerPatient(
                new PatientRegistrationRequest("Hacker", "Malicious", null, null, "555-0999", null, null, null, null),
                patientPrincipal))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403 FORBIDDEN");
    }

    @Test
    void searchPatients_byName() {
        patientService.registerPatient(
                new PatientRegistrationRequest("Alice", "Smith", null, null, "555-0200", null, null, null, null),
                receptionistPrincipal);
        patientService.registerPatient(
                new PatientRegistrationRequest("Bob", "Jones", null, null, "555-0201", null, null, null, null),
                receptionistPrincipal);

        var page = patientService.searchPatients("Alice", PageRequest.of(0, 20), receptionistPrincipal);
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).firstName()).isEqualTo("Alice");
    }

    @Test
    void searchPatients_emptyQuery_returnsAll() {
        patientService.registerPatient(
                new PatientRegistrationRequest("Alice", "Smith", null, null, "555-0300", null, null, null, null),
                receptionistPrincipal);
        patientService.registerPatient(
                new PatientRegistrationRequest("Bob", "Jones", null, null, "555-0301", null, null, null, null),
                receptionistPrincipal);

        assertThat(patientService.searchPatients(null, PageRequest.of(0, 20), receptionistPrincipal).getContent()).hasSize(2);
        assertThat(patientService.searchPatients("", PageRequest.of(0, 20), receptionistPrincipal).getContent()).hasSize(2);
    }
}

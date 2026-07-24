package com.healthtrack.service;

import com.healthtrack.dto.EmrDtos.*;
import com.healthtrack.entity.*;
import com.healthtrack.repository.*;
import com.healthtrack.security.UserPrincipal;
import com.healthtrack.support.PostgresTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MedicalRecordServiceTest extends PostgresTestBase {

    @Autowired private MedicalRecordService medicalRecordService;
    @Autowired private HospitalRepository hospitalRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private MedicalRecordRepository medicalRecordRepository;
    @Autowired private LabTestOrderRepository labTestOrderRepository;

    private Hospital hospital;
    private Patient patient;
    private UserPrincipal doctorPrincipal;
    private UserPrincipal patientPrincipal;

    @BeforeEach
    void setUp() {
        hospital = hospitalRepository.save(Hospital.builder()
                .name("EMR Test Hospital").licenseNumber("EMR-" + System.nanoTime())
                .contactEmail("emr@test.com")
                .subscriptionTier(SubscriptionTier.MONTHLY)
                .subscriptionStatus(SubscriptionStatus.ACTIVE).build());

        User docUser = userRepository.save(User.builder()
                .fullName("Dr. Diagnose").email("doc-emr-" + System.nanoTime() + "@test.com")
                .passwordHash("x").role(Role.DOCTOR).hospital(hospital).build());

        User patUser = userRepository.save(User.builder()
                .fullName("EMR Patient").email("pat-emr-" + System.nanoTime() + "@test.com")
                .passwordHash("x").role(Role.PATIENT).hospital(hospital).build());

        doctorPrincipal = new UserPrincipal(docUser);
        patientPrincipal = new UserPrincipal(patUser);

        patient = patientRepository.save(Patient.builder()
                .hospital(hospital).firstName("EMR").lastName("Patient")
                .phoneNumber("555-EMR-1").build());
    }

    @Test
    void createRecord_withPrescriptions() {
        CreateMedicalRecordRequest request = new CreateMedicalRecordRequest(
                patient.getId(), null, LocalDate.now(),
                "Headache", "Normal", "Migraine", null,
                List.of(new PrescriptionItemRequest("Sumatriptan", "50mg", "Once daily", "7 days", "After food")),
                null);

        MedicalRecordResponse response = medicalRecordService.createRecord(request, doctorPrincipal);

        assertThat(response.chiefComplaints()).isEqualTo("Headache");
        assertThat(response.diagnosis()).isEqualTo("Migraine");
        assertThat(response.prescriptions()).hasSize(1);
        assertThat(response.prescriptions().get(0).medicineName()).isEqualTo("Sumatriptan");
    }

    @Test
    void createRecord_withLabRequest() {
        CreateMedicalRecordRequest request = new CreateMedicalRecordRequest(
                patient.getId(), null, LocalDate.now(),
                "Chest pain", "Elevated BP", "Hypertension", null,
                null,
                List.of(new ServiceRequestEntry("LAB_TEST", "Complete Blood Count")));

        MedicalRecordResponse response = medicalRecordService.createRecord(request, doctorPrincipal);

        assertThat(response.serviceRequests()).hasSize(1);
        assertThat(response.serviceRequests().get(0).serviceType()).isEqualTo("LAB_TEST");

        boolean labOrderCreated = labTestOrderRepository.findByPatientIdOrderByCreatedAtDesc(patient.getId())
                .stream().anyMatch(o -> o.getTestName().equals("Complete Blood Count"));
        assertThat(labOrderCreated).isTrue();
    }

    @Test
    void createRecord_nonDoctor_throws403() {
        assertThatThrownBy(() -> medicalRecordService.createRecord(
                new CreateMedicalRecordRequest(patient.getId(), null, LocalDate.now(),
                        "Cough", null, null, null, null, null),
                patientPrincipal))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403 FORBIDDEN");
    }

    @Test
    void getPatientHistory_returnsRecords() {
        medicalRecordService.createRecord(
                new CreateMedicalRecordRequest(patient.getId(), null, LocalDate.now(),
                        "Visit 1", null, "Diagnosis 1", null, null, null),
                doctorPrincipal);
        medicalRecordService.createRecord(
                new CreateMedicalRecordRequest(patient.getId(), null, LocalDate.now().minusDays(1),
                        "Visit 2", null, "Diagnosis 2", null, null, null),
                doctorPrincipal);

        var history = medicalRecordService.getPatientHistory(patient.getId(), doctorPrincipal);
        assertThat(history).hasSize(2);
    }
}

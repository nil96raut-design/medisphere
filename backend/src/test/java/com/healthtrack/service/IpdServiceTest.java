package com.healthtrack.service;

import com.healthtrack.dto.IpdDtos.*;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IpdServiceTest extends PostgresTestBase {

    @Autowired private IpdService ipdService;
    @Autowired private HospitalRepository hospitalRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private BedRepository bedRepository;
    @Autowired private DoctorRepository doctorRepository;

    private Hospital hospital;
    private Patient patient;
    private Bed bed;
    private UserPrincipal doctorPrincipal;
    private UserPrincipal nursePrincipal;
    private UserPrincipal patientPrincipal;
    private Long doctorId;

    @BeforeEach
    void setUp() {
        hospital = hospitalRepository.save(Hospital.builder()
                .name("Test Hospital").licenseNumber("IPD-" + System.nanoTime())
                .contactEmail("ipd@test.com")
                .subscriptionTier(SubscriptionTier.MONTHLY)
                .subscriptionStatus(SubscriptionStatus.ACTIVE).build());

        User docUser = userRepository.save(User.builder()
                .fullName("Dr. Healer").email("doc-ipd-" + System.nanoTime() + "@test.com")
                .passwordHash("x").role(Role.DOCTOR).hospital(hospital).build());

        User nurseUser = userRepository.save(User.builder()
                .fullName("Nurse Joy").email("nurse-" + System.nanoTime() + "@test.com")
                .passwordHash("x").role(Role.NURSE).hospital(hospital).build());

        User patUser = userRepository.save(User.builder()
                .fullName("Sick Patient").email("pat-ipd-" + System.nanoTime() + "@test.com")
                .passwordHash("x").role(Role.PATIENT).hospital(hospital).build());

        doctorPrincipal = new UserPrincipal(docUser);
        nursePrincipal = new UserPrincipal(nurseUser);
        patientPrincipal = new UserPrincipal(patUser);

        patient = patientRepository.save(Patient.builder()
                .hospital(hospital).firstName("Sick").lastName("Patient")
                .phoneNumber("555-IPD-1").build());

        bed = bedRepository.save(Bed.builder()
                .hospital(hospital).wardName("General Ward")
                .bedNumber("G-01").chargePerDay(new BigDecimal("500"))
                .isOccupied(false).build());

        Doctor doctor = doctorRepository.save(Doctor.builder()
                .hospital(hospital).user(docUser)
                .specialization("General Medicine")
                .consultationFee(new BigDecimal("200"))
                .isAvailable(true).build());
        doctorId = docUser.getId(); // AdmissionRequest.doctorId() expects User ID, not Doctor entity ID
    }

    @Test
    void admitPatient_createsAdmission() {
        AdmissionResponse response = ipdService.admitPatient(
                new AdmissionRequest(patient.getId(), doctorId, bed.getId(), LocalDate.now(), "Fever"),
                doctorPrincipal);

        assertThat(response.patientName()).contains("Sick");
        assertThat(response.status()).isEqualTo("ADMITTED");
        assertThat(response.bedId()).isEqualTo(bed.getId());
    }

    @Test
    void admitPatient_occupiedBed_throws409() {
        ipdService.admitPatient(
                new AdmissionRequest(patient.getId(), doctorId, bed.getId(), LocalDate.now(), "First"),
                doctorPrincipal);

        Patient patient2 = patientRepository.save(Patient.builder()
                .hospital(hospital).firstName("Second").lastName("Patient")
                .phoneNumber("555-IPD-2").build());

        assertThatThrownBy(() -> ipdService.admitPatient(
                new AdmissionRequest(patient2.getId(), doctorId, bed.getId(), LocalDate.now(), "Second"),
                doctorPrincipal))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409 CONFLICT");
    }

    @Test
    void admitPatient_nonDoctor_throws403() {
        assertThatThrownBy(() -> ipdService.admitPatient(
                new AdmissionRequest(patient.getId(), doctorId, bed.getId(), LocalDate.now(), "Nope"),
                patientPrincipal))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403 FORBIDDEN");
    }

    @Test
    void discharge_releasesBed() {
        AdmissionResponse admission = ipdService.admitPatient(
                new AdmissionRequest(patient.getId(), doctorId, bed.getId(), LocalDate.now(), "Chest pain"),
                doctorPrincipal);

        AdmissionResponse discharged = ipdService.discharge(admission.id(),
                new DischargeRequest("Recovered"), doctorPrincipal);

        assertThat(discharged.status()).isEqualTo("DISCHARGED");
        assertThat(bedRepository.findById(bed.getId()).orElseThrow().getIsOccupied()).isFalse();
    }

    @Test
    void discharge_alreadyDischarged_throws400() {
        AdmissionResponse admission = ipdService.admitPatient(
                new AdmissionRequest(patient.getId(), doctorId, bed.getId(), LocalDate.now(), "Test"),
                doctorPrincipal);
        ipdService.discharge(admission.id(), new DischargeRequest("Done"), doctorPrincipal);

        assertThatThrownBy(() -> ipdService.discharge(admission.id(),
                new DischargeRequest("Again"), doctorPrincipal))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST");
    }

    @Test
    void addNursingLog_allowsNurse() {
        AdmissionResponse admission = ipdService.admitPatient(
                new AdmissionRequest(patient.getId(), doctorId, bed.getId(), LocalDate.now(), "Observation"),
                doctorPrincipal);

        NursingLogResponse log = ipdService.addNursingLog(admission.id(),
                new NursingLogRequest("BP 120/80", "Paracetamol", "Resting comfortably"),
                nursePrincipal);

        assertThat(log.vitalsRecorded()).isEqualTo("BP 120/80");
        assertThat(log.nurseName()).contains("Joy");
    }

    @Test
    void getActiveAdmissions_returnsAdmitted() {
        ipdService.admitPatient(
                new AdmissionRequest(patient.getId(), doctorId, bed.getId(), LocalDate.now(), "Admit"),
                doctorPrincipal);

        var active = ipdService.getActiveAdmissions(doctorPrincipal);
        assertThat(active).isNotEmpty();
        assertThat(active.get(0).status()).isEqualTo("ADMITTED");
    }
}

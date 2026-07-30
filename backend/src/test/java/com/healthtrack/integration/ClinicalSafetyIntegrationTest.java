package com.healthtrack.integration;

import com.healthtrack.dto.ClinicalSafetyDtos.ShiftHandoverRequest;
import com.healthtrack.entity.*;
import com.healthtrack.repository.*;
import com.healthtrack.security.UserPrincipal;
import com.healthtrack.service.*;
import com.healthtrack.support.PostgresTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClinicalSafetyIntegrationTest extends PostgresTestBase {

    @Autowired private MedicationSchedulingService medicationSchedulingService;
    @Autowired private AlertService alertService;
    @Autowired private ShiftHandoverService shiftHandoverService;
    @Autowired private BedCleaningService bedCleaningService;
    @Autowired private IpdService ipdService;
    @Autowired private NurseService nurseService;
    @Autowired private MedicalRecordService medicalRecordService;
    @Autowired private HospitalRepository hospitalRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private DoctorRepository doctorRepository;
    @Autowired private BedRepository bedRepository;
    @Autowired private PrescriptionItemRepository prescriptionItemRepository;
    @Autowired private MedicationScheduleRepository medicationScheduleRepository;
    @Autowired private AlertRepository alertRepository;
    @Autowired private BedCleaningRepository bedCleaningRepository;

    private Hospital hospital;
    private Patient patient;
    private User doctor;
    private User nurse1;
    private User nurse2;
    private UserPrincipal doctorPrincipal;
    private UserPrincipal nursePrincipal1;
    private UserPrincipal nursePrincipal2;
    private Doctor doctorProfile;
    private Bed bed;

    @BeforeEach
    void setUp() {
        hospital = hospitalRepository.save(Hospital.builder()
                .name("Clinical Safety Hospital").licenseNumber("CS-" + System.nanoTime())
                .contactEmail("cs@test.com").subscriptionTier(SubscriptionTier.MONTHLY)
                .subscriptionStatus(SubscriptionStatus.ACTIVE).build());

        doctor = userRepository.save(User.builder()
                .fullName("Dr. Safety").email("doc-cs-" + System.nanoTime() + "@test.com")
                .passwordHash("x").role(Role.DOCTOR).hospital(hospital).build());

        nurse1 = userRepository.save(User.builder()
                .fullName("Nurse One").email("n1-" + System.nanoTime() + "@test.com")
                .passwordHash("x").role(Role.NURSE).hospital(hospital).build());

        nurse2 = userRepository.save(User.builder()
                .fullName("Nurse Two").email("n2-" + System.nanoTime() + "@test.com")
                .passwordHash("x").role(Role.NURSE).hospital(hospital).build());

        doctorPrincipal = new UserPrincipal(doctor);
        nursePrincipal1 = new UserPrincipal(nurse1);
        nursePrincipal2 = new UserPrincipal(nurse2);

        patient = patientRepository.save(Patient.builder()
                .hospital(hospital).firstName("Clinical").lastName("Patient")
                .phoneNumber("CS-PAT").build());

        doctorProfile = doctorRepository.save(Doctor.builder()
                .hospital(hospital).user(doctor)
                .specialization("General")
                .consultationFee(new java.math.BigDecimal("100"))
                .isAvailable(true).build());

        bed = bedRepository.save(Bed.builder()
                .hospital(hospital).wardName("CS Ward").bedNumber("CS-101")
                .chargePerDay(new java.math.BigDecimal("50")).isOccupied(false).build());
    }

    @Test
    void fullClinicalSafetyFlow() {
        // 1. Admit patient
        var admissionRequest = new com.healthtrack.dto.IpdDtos.AdmissionRequest(
                patient.getId(), doctor.getId(), bed.getId(), LocalDate.now(), "Test admission");
        var admission = ipdService.admitPatient(admissionRequest, doctorPrincipal);
        assertThat(admission.status()).isEqualTo("ADMITTED");

        // 2. Nurse assigned to patient
        var assignResponse = nurseService.assignNurse(
                new com.healthtrack.dto.NurseDtos.AssignNurseRequest(
                        nurse1.getId(), patient.getId(), bed.getId()),
                nursePrincipal1);
        assertThat(assignResponse.patientId()).isEqualTo(patient.getId());

        // 3. Medication schedule auto-generated via MedicalRecord
        var medRecordRequest = new com.healthtrack.dto.EmrDtos.CreateMedicalRecordRequest(
                patient.getId(), null, LocalDate.now(), "Chest pain", "Normal",
                "Hypertension", null,
                List.of(new com.healthtrack.dto.EmrDtos.PrescriptionItemRequest(
                        "Metoprolol", "50mg", "2 times/day", "30 days", "Take with food")),
                null);
        var medRecord = medicalRecordService.createRecord(medRecordRequest, doctorPrincipal);
        assertThat(medRecord.prescriptions()).isNotEmpty();

        // 4. Medication schedules auto-generated via MedicalRecord.createRecord
        var prescriptionId = medRecord.prescriptions().get(0).id();
        var schedulesFromRepo = medicationScheduleRepository
                .findByPatientIdOrderByScheduledTimeAsc(patient.getId());
        assertThat(schedulesFromRepo).isNotEmpty();
        assertThat(schedulesFromRepo.get(0).getStatus()).isEqualTo(MedicationScheduleStatus.PENDING);

        // 5. Alert created for critical event
        var alert = alertService.createAlert(hospital.getId(), patient.getId(),
                AlertType.VITAL, AlertSeverity.CRITICAL, "Critical: HR > 140");
        assertThat(alert.status()).isEqualTo(AlertStatus.ACTIVE);

        // 6. Alert acknowledged by nurse
        var acknowledged = alertService.acknowledgeAlert(alert.id(), nursePrincipal1);
        assertThat(acknowledged.status()).isEqualTo(AlertStatus.ACKNOWLEDGED);

        // 7. Shift handover
        var handoverRequest = new ShiftHandoverRequest(
                nurse2.getId(), "CS Ward", "All patients stable", "1 patient recovering");
        var handover = shiftHandoverService.submitHandover(handoverRequest, nursePrincipal1);
        assertThat(handover.toNurseId()).isEqualTo(nurse2.getId());

        // 8. Discharge triggers bed cleaning
        var dischargeRequest = new com.healthtrack.dto.IpdDtos.DischargeRequest("Patient recovered");
        ipdService.discharge(admission.id(), dischargeRequest, doctorPrincipal);

        var cleaningRequests = bedCleaningService.getPendingCleaningRequests(nursePrincipal2);
        assertThat(cleaningRequests).isNotEmpty();

        // 9. Mark bed as cleaned
        var cleaningId = cleaningRequests.get(0).id();
        var cleaned = bedCleaningService.markCleaned(cleaningId, nursePrincipal2);
        assertThat(cleaned.status()).isEqualTo(CleaningStatus.CLEANED);

        // 10. Bed available again
        var availableBeds = ipdService.getAvailableBeds(doctorPrincipal);
        assertThat(availableBeds).anyMatch(b -> b.id().equals(bed.getId()));
    }
}

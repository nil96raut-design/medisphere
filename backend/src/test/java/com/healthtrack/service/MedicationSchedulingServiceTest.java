package com.healthtrack.service;

import com.healthtrack.entity.*;
import com.healthtrack.repository.*;
import com.healthtrack.support.PostgresTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MedicationSchedulingServiceTest extends PostgresTestBase {

    @Autowired private MedicationSchedulingService medicationSchedulingService;
    @Autowired private MedicationScheduleRepository medicationScheduleRepository;
    @Autowired private PrescriptionItemRepository prescriptionItemRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private HospitalRepository hospitalRepository;
    @Autowired private MedicalRecordRepository medicalRecordRepository;
    @Autowired private UserRepository userRepository;

    private PrescriptionItem item;
    private Patient patient;

    @BeforeEach
    void setUp() {
        Hospital hospital = hospitalRepository.save(Hospital.builder()
                .name("MedSched Hospital").licenseNumber("MS-" + System.nanoTime())
                .contactEmail("ms@test.com").subscriptionTier(SubscriptionTier.MONTHLY)
                .subscriptionStatus(SubscriptionStatus.ACTIVE).build());

        patient = patientRepository.save(Patient.builder()
                .hospital(hospital).firstName("Test").lastName("Patient")
                .phoneNumber("MS-TEST").build());

        User doctor = userRepository.save(User.builder()
                .fullName("Dr. MedSched").email("ms-doc-" + System.nanoTime() + "@test.com")
                .passwordHash("x").role(Role.DOCTOR).hospital(hospital).build());

        MedicalRecord record = medicalRecordRepository.save(MedicalRecord.builder()
                .hospital(hospital).patient(patient).doctor(doctor)
                .encounterDate(LocalDate.now())
                .chiefComplaints("Test").build());

        item = prescriptionItemRepository.save(PrescriptionItem.builder()
                .medicalRecord(record).hospital(hospital)
                .medicineName("TestMed").dosage("500mg")
                .frequency("3 times/day").duration("7 days")
                .build());
    }

    @Test
    void generateSchedules_createsCorrectNumberOfSchedules() {
        var schedules = medicationSchedulingService.generateSchedules(item.getId());
        assertThat(schedules).isNotEmpty();
        assertThat(schedules.get(0).prescriptionItemId()).isEqualTo(item.getId());
    }

    @Test
    void markGiven_updatesStatus() {
        MedicationSchedule schedule = medicationScheduleRepository.save(MedicationSchedule.builder()
                .prescriptionItemId(item.getId()).patientId(patient.getId())
                .scheduledTime(OffsetDateTime.now().minusMinutes(10))
                .status(MedicationScheduleStatus.PENDING)
                .build());

        var result = medicationSchedulingService.markGiven(schedule.getId(), 1L);
        assertThat(result.status()).isEqualTo(MedicationScheduleStatus.GIVEN);
    }

    @Test
    void markGiven_duplicateThrows() {
        MedicationSchedule schedule = medicationScheduleRepository.save(MedicationSchedule.builder()
                .prescriptionItemId(item.getId()).patientId(patient.getId())
                .scheduledTime(OffsetDateTime.now().minusMinutes(10))
                .status(MedicationScheduleStatus.PENDING)
                .build());

        medicationSchedulingService.markGiven(schedule.getId(), 1L);
        try {
            medicationSchedulingService.markGiven(schedule.getId(), 1L);
        } catch (IllegalStateException e) {
            assertThat(e.getMessage()).contains("already");
        }
    }

    @Test
    void detectMissedDoses_marksOverdueSchedules() {
        MedicationSchedule overdue = medicationScheduleRepository.save(MedicationSchedule.builder()
                .prescriptionItemId(item.getId()).patientId(patient.getId())
                .scheduledTime(OffsetDateTime.now().minusHours(2))
                .status(MedicationScheduleStatus.PENDING)
                .build());

        medicationSchedulingService.detectMissedDoses();

        var updated = medicationScheduleRepository.findById(overdue.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(MedicationScheduleStatus.MISSED);
    }
}

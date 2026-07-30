package com.healthtrack.service;

import com.healthtrack.entity.*;
import com.healthtrack.repository.*;
import com.healthtrack.support.PostgresTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TaskEngineServiceTest extends PostgresTestBase {

    @Autowired private TaskEngineService taskEngineService;
    @Autowired private NurseTaskRepository nurseTaskRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private HospitalRepository hospitalRepository;
    @Autowired private UserRepository userRepository;

    private Hospital hospital;
    private Patient patient;
    private User nurse;
    private NurseTask recurringTask;

    @BeforeEach
    void setUp() {
        hospital = hospitalRepository.save(Hospital.builder()
                .name("TaskEng Hospital").licenseNumber("TE-" + System.nanoTime())
                .contactEmail("te@test.com").subscriptionTier(SubscriptionTier.MONTHLY)
                .subscriptionStatus(SubscriptionStatus.ACTIVE).build());

        patient = patientRepository.save(Patient.builder()
                .hospital(hospital).firstName("Task").lastName("Eng")
                .phoneNumber("TE-TEST").build());

        nurse = userRepository.save(User.builder()
                .fullName("Nurse TaskEng").email("nurse-te-" + System.nanoTime() + "@test.com")
                .passwordHash("x").role(Role.NURSE).hospital(hospital).build());

        recurringTask = nurseTaskRepository.save(NurseTask.builder()
                .hospital(hospital).nurse(nurse).patient(patient)
                .taskType(NurseTaskType.VITALS)
                .dueTime(OffsetDateTime.now().minusHours(2))
                .status(NurseTaskStatus.DONE)
                .completedAt(OffsetDateTime.now())
                .isRecurring(true)
                .recurrenceIntervalMinutes(240)
                .priority(TaskPriority.NORMAL)
                .source("auto")
                .build());
    }

    @Test
    void generateRecurringTasks_createsNextTask() {
        taskEngineService.generateRecurringTasks();
        var tasks = nurseTaskRepository.findByPatientIdOrderByCreatedAtDesc(patient.getId());
        assertThat(tasks).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void markOverdueTasks_updatesPriority() {
        NurseTask overdue = nurseTaskRepository.save(NurseTask.builder()
                .hospital(hospital).nurse(nurse).patient(patient)
                .taskType(NurseTaskType.OBSERVATION)
                .dueTime(OffsetDateTime.now().minusHours(2))
                .status(NurseTaskStatus.PENDING)
                .priority(TaskPriority.NORMAL)
                .build());

        taskEngineService.markOverdueTasks();
        var updated = nurseTaskRepository.findById(overdue.getId()).orElseThrow();
        assertThat(updated.getPriority()).isEqualTo(TaskPriority.HIGH);
    }
}

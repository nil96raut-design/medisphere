package com.healthtrack.service;

import com.healthtrack.dto.TaskDtos.CreateTaskRequest;
import com.healthtrack.dto.TaskDtos.TaskResponse;
import com.healthtrack.dto.TaskDtos.UpdateProgressRequest;
import com.healthtrack.entity.*;
import com.healthtrack.repository.HospitalRepository;
import com.healthtrack.repository.UserRepository;
import com.healthtrack.support.PostgresTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskServiceTest extends PostgresTestBase {

    @Autowired
    private TaskService taskService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    private Hospital hospital;
    private User doctor;
    private User patient;
    private User otherPatient;

    @BeforeEach
    void setUp() {
        hospital = hospitalRepository.save(Hospital.builder()
                .name("Task Test Hospital").licenseNumber("TASK-" + System.nanoTime())
                .contactEmail("task@test.com")
                .subscriptionTier(SubscriptionTier.MONTHLY)
                .subscriptionStatus(SubscriptionStatus.ACTIVE).build());

        doctor = userRepository.save(User.builder()
                .fullName("Dr. House").email("house-" + System.nanoTime() + "@example.com")
                .passwordHash("x").role(Role.DOCTOR).hospital(hospital).build());
        patient = userRepository.save(User.builder()
                .fullName("Jane Patient").email("jane-" + System.nanoTime() + "@example.com")
                .passwordHash("x").role(Role.PATIENT).hospital(hospital).build());
        otherPatient = userRepository.save(User.builder()
                .fullName("Other Patient").email("other-" + System.nanoTime() + "@example.com")
                .passwordHash("x").role(Role.PATIENT).hospital(hospital).build());
    }

    @Test
    void createTask_assignsToPatient() {
        TaskResponse task = taskService.createTask(
                new CreateTaskRequest("Take medication", "twice daily", patient.getId(), TaskPriority.HIGH, null),
                doctor);

        assertThat(task.assigneeId()).isEqualTo(patient.getId());
        assertThat(task.status()).isEqualTo(TaskStatus.NOT_STARTED);
        assertThat(task.progressPercent()).isZero();
    }

    @Test
    void listTasksFor_patient_onlySeesOwnTasks() {
        taskService.createTask(new CreateTaskRequest("Task A", null, patient.getId(), null, null), doctor);
        taskService.createTask(new CreateTaskRequest("Task B", null, otherPatient.getId(), null, null), doctor);

        var patientTasks = taskService.listTasksFor(patient);

        assertThat(patientTasks).hasSize(1);
        assertThat(patientTasks.get(0).title()).isEqualTo("Task A");
    }

    @Test
    void listTasksFor_doctor_seesAllTasks() {
        taskService.createTask(new CreateTaskRequest("Task A", null, patient.getId(), null, null), doctor);
        taskService.createTask(new CreateTaskRequest("Task B", null, otherPatient.getId(), null, null), doctor);

        assertThat(taskService.listTasksFor(doctor)).hasSize(2);
    }

    @Test
    void updateProgress_clampsPercentAndLogsTimeline() {
        TaskResponse task = taskService.createTask(
                new CreateTaskRequest("Walk 30 minutes", null, patient.getId(), null, null), doctor);

        TaskResponse updated = taskService.updateProgress(task.id(),
                new UpdateProgressRequest(TaskStatus.IN_PROGRESS, 150, "felt good"), patient);

        assertThat(updated.progressPercent()).isEqualTo(100); // clamped
        assertThat(updated.status()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(taskService.getTimeline(task.id(), patient)).hasSize(1);
    }

    @Test
    void updateProgress_byUnrelatedPatient_isForbidden() {
        TaskResponse task = taskService.createTask(
                new CreateTaskRequest("Take medication", null, patient.getId(), null, null), doctor);

        assertThatThrownBy(() -> taskService.updateProgress(task.id(),
                new UpdateProgressRequest(TaskStatus.COMPLETED, 100, "not mine"), otherPatient))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void getTimeline_byUnrelatedPatient_isForbidden() {
        TaskResponse task = taskService.createTask(
                new CreateTaskRequest("Take medication", null, patient.getId(), null, null), doctor);

        assertThatThrownBy(() -> taskService.getTimeline(task.id(), otherPatient))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void getTimeline_byOwnerOrClinicalStaff_isAllowed() {
        TaskResponse task = taskService.createTask(
                new CreateTaskRequest("Take medication", null, patient.getId(), null, null), doctor);

        assertThat(taskService.getTimeline(task.id(), patient)).isNotNull();
        assertThat(taskService.getTimeline(task.id(), doctor)).isNotNull();
    }

    @Test
    void searchTasksFor_filtersByTitle() {
        taskService.createTask(new CreateTaskRequest("Blood pressure check", null, patient.getId(), null, null), doctor);
        taskService.createTask(new CreateTaskRequest("Refill prescription", null, patient.getId(), null, null), doctor);

        var results = taskService.searchTasksFor(doctor, "blood",
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")));

        assertThat(results.getTotalElements()).isEqualTo(1);
        assertThat(results.getContent().get(0).title()).isEqualTo("Blood pressure check");
    }

    @Test
    void searchTasksFor_paginates() {
        for (int i = 0; i < 5; i++) {
            taskService.createTask(new CreateTaskRequest("Task " + i, null, patient.getId(), null, null), doctor);
        }

        var page0 = taskService.searchTasksFor(doctor, null, PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "createdAt")));

        assertThat(page0.getContent()).hasSize(2);
        assertThat(page0.getTotalElements()).isEqualTo(5);
        assertThat(page0.getTotalPages()).isEqualTo(3);
    }
}

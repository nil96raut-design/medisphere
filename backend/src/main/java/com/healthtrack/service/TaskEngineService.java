package com.healthtrack.service;

import com.healthtrack.dto.NurseDtos.NurseTaskResponse;
import com.healthtrack.entity.*;
import com.healthtrack.event.EventConstants;
import com.healthtrack.event.EventPublisher;
import com.healthtrack.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TaskEngineService {

    private static final Logger log = LoggerFactory.getLogger(TaskEngineService.class);

    private final NurseTaskRepository nurseTaskRepository;
    private final NurseAssignmentRepository nurseAssignmentRepository;
    private final EventPublisher eventPublisher;

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void generateRecurringTasks() {
        List<NurseTask> completedRecurring = nurseTaskRepository
                .findByIsRecurringTrueAndStatus(NurseTaskStatus.DONE);

        for (NurseTask completed : completedRecurring) {
            if (completed.getRecurrenceIntervalMinutes() == null) continue;

            boolean alreadyScheduled = nurseTaskRepository
                    .findByPatientIdOrderByCreatedAtDesc(completed.getPatient().getId())
                    .stream()
                    .anyMatch(t -> t.getStatus() == NurseTaskStatus.PENDING
                            && t.getTaskType() == completed.getTaskType()
                            && t.getNurse().getId().equals(completed.getNurse().getId())
                            && t.getSource() != null
                            && t.getSource().equals("recurring:" + completed.getId()));

            if (alreadyScheduled) continue;

            NurseTask nextTask = NurseTask.builder()
                    .hospital(completed.getHospital())
                    .nurse(completed.getNurse())
                    .patient(completed.getPatient())
                    .taskType(completed.getTaskType())
                    .dueTime(OffsetDateTime.now().plusMinutes(completed.getRecurrenceIntervalMinutes()))
                    .isRecurring(false)
                    .priority(completed.getPriority())
                    .source("recurring:" + completed.getId())
                    .build();
            nextTask = nurseTaskRepository.save(nextTask);

            eventPublisher.publish(EventConstants.RECURRING_TASK_CREATED,
                    completed.getHospital().getId(),
                    Map.of("taskId", nextTask.getId(), "patientId", completed.getPatient().getId(),
                            "taskType", completed.getTaskType().name(),
                            "hospitalId", completed.getHospital().getId()));
        }

        if (!completedRecurring.isEmpty()) {
            log.debug("Generated {} recurring tasks", completedRecurring.size());
        }
    }

    @Scheduled(fixedRate = 120000)
    @Transactional
    public void markOverdueTasks() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusHours(1);
        List<NurseTask> overdue = nurseTaskRepository.findOverdueTasks(cutoff);

        for (NurseTask task : overdue) {
            task.setPriority(TaskPriority.HIGH);
            nurseTaskRepository.save(task);
        }

        if (!overdue.isEmpty()) {
            log.warn("Marked {} tasks as overdue (priority HIGH)", overdue.size());
        }
    }

    @Transactional
    public NurseTask createRecurringTask(NurseTask prototype, int intervalMinutes) {
        NurseTask task = NurseTask.builder()
                .hospital(prototype.getHospital())
                .nurse(prototype.getNurse())
                .patient(prototype.getPatient())
                .taskType(prototype.getTaskType())
                .dueTime(prototype.getDueTime())
                .isRecurring(true)
                .recurrenceIntervalMinutes(intervalMinutes)
                .priority(prototype.getPriority() != null ? prototype.getPriority() : TaskPriority.NORMAL)
                .source("auto")
                .build();
        return nurseTaskRepository.save(task);
    }
}

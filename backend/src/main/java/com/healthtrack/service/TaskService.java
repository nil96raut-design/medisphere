package com.healthtrack.service;

import com.healthtrack.dto.TaskDtos.*;
import com.healthtrack.entity.*;
import com.healthtrack.repository.ProgressNoteRepository;
import com.healthtrack.repository.TaskRepository;
import com.healthtrack.repository.UserRepository;
import com.healthtrack.security.TenantValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProgressNoteRepository progressNoteRepository;
    private final TenantValidator tenantValidator;

    @Transactional
    public TaskResponse createTask(CreateTaskRequest req, User creator) {
        User assignee = userRepository.findById(req.assigneeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assignee not found"));

        Task task = Task.builder()
                .title(req.title())
                .description(req.description())
                .assignee(assignee)
                .assignedBy(creator)
                .hospital(creator.getHospital())
                .priority(req.priority() == null ? TaskPriority.MEDIUM : req.priority())
                .dueDate(req.dueDate())
                .status(TaskStatus.NOT_STARTED)
                .progressPercent(0)
                .build();

        return toResponse(taskRepository.save(task));
    }

    // Doctors/staff see everything they assigned; patients see only their own tasks.
    @Transactional(readOnly = true)
    public List<TaskResponse> listTasksFor(User viewer) {
        List<Task> tasks = switch (viewer.getRole()) {
            case PATIENT -> taskRepository.findByAssigneeId(viewer.getId());
            case DOCTOR, RECEPTIONIST, ADMIN, NURSE, PHARMACIST, LAB_TECH -> taskRepository.findAllByOrderByCreatedAtDesc();
        };
        return tasks.stream().map(this::toResponse).toList();
    }

    // Paginated, searchable version of listTasksFor. Kept listTasksFor in place
    // so nothing else that depends on it breaks; new callers should use this one.
    @Transactional(readOnly = true)
    public Page<TaskResponse> searchTasksFor(User viewer, String q, Pageable pageable) {
        Page<Task> page = switch (viewer.getRole()) {
            case PATIENT -> taskRepository.searchByAssignee(viewer.getId(), q, pageable);
            case DOCTOR, RECEPTIONIST, ADMIN, NURSE, PHARMACIST, LAB_TECH -> taskRepository.searchAll(q, pageable);
        };
        return page.map(this::toResponse);
    }

    @Transactional
    public TaskResponse updateProgress(Long taskId, UpdateProgressRequest req, User actor) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
        tenantValidator.validateHospitalAccess(task.getHospital().getId(), actor.getHospital().getId());

        boolean isOwner = task.getAssignee().getId().equals(actor.getId());
        boolean isClinicalStaff = actor.getRole() == Role.DOCTOR || actor.getRole() == Role.RECEPTIONIST || actor.getRole() == Role.ADMIN;
        if (!isOwner && !isClinicalStaff) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to update this task");
        }

        int clampedPercent = Math.max(0, Math.min(100, req.progressPercent()));
        task.setStatus(req.status());
        task.setProgressPercent(clampedPercent);
        Task saved = taskRepository.save(task);

        progressNoteRepository.save(ProgressNote.builder()
                .task(saved)
                .author(actor)
                .hospital(task.getHospital())
                .note(req.note())
                .progressPercent(clampedPercent)
                .status(req.status())
                .build());

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ProgressNoteResponse> getTimeline(Long taskId, User viewer) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
        tenantValidator.validateHospitalAccess(task.getHospital().getId(), viewer.getHospital().getId());

        boolean isOwner = task.getAssignee().getId().equals(viewer.getId());
        boolean isClinicalStaff = viewer.getRole() == Role.DOCTOR || viewer.getRole() == Role.RECEPTIONIST || viewer.getRole() == Role.ADMIN;
        if (!isOwner && !isClinicalStaff) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to view this task's timeline");
        }

        return progressNoteRepository.findByTaskIdOrderByCreatedAtDesc(taskId).stream()
                .map(pn -> new ProgressNoteResponse(
                        pn.getId(), pn.getTask().getId(), pn.getAuthor().getId(),
                        pn.getAuthor().getFullName(), pn.getNote(),
                        pn.getProgressPercent(), pn.getStatus(), pn.getCreatedAt()))
                .toList();
    }

    private TaskResponse toResponse(Task t) {
        return new TaskResponse(
                t.getId(), t.getTitle(), t.getDescription(),
                t.getAssignee().getId(), t.getAssignee().getFullName(),
                t.getAssignedBy().getId(), t.getAssignedBy().getFullName(),
                t.getStatus(), t.getPriority(), t.getProgressPercent(), t.getDueDate()
        );
    }
}

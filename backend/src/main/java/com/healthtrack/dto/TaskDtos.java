package com.healthtrack.dto;

import com.healthtrack.entity.TaskPriority;
import com.healthtrack.entity.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDate;

public class TaskDtos {

    public record CreateTaskRequest(
            @NotBlank String title,
            String description,
            @NotNull Long assigneeId,
            TaskPriority priority,
            LocalDate dueDate
    ) {}

    public record UpdateProgressRequest(
            @NotNull TaskStatus status,
            @NotNull Integer progressPercent,
            String note
    ) {}

    public record TaskResponse(
            Long id,
            String title,
            String description,
            Long assigneeId,
            String assigneeName,
            Long assignedById,
            String assignedByName,
            TaskStatus status,
            TaskPriority priority,
            Integer progressPercent,
            LocalDate dueDate
    ) {}

    public record ProgressNoteResponse(
            Long id,
            Long taskId,
            Long authorId,
            String authorName,
            String note,
            Integer progressPercent,
            TaskStatus status,
            Instant createdAt
    ) {}
}

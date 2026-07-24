package com.healthtrack.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class LabDtos {

    public record LabOrderResponse(
            Long id,
            Long patientId,
            String patientName,
            String testName,
            String requestedByName,
            String status,
            String resultValues,
            String technicianNotes,
            LocalDateTime completedAt,
            LocalDateTime createdAt
    ) {}

    public record SampleCollectionRequest(
            @NotBlank String technicianNotes
    ) {}

    public record ResultEntryRequest(
            @NotBlank String resultValues,
            String technicianNotes
    ) {}
}

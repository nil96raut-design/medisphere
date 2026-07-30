package com.healthtrack.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class PatientDtos {

    public record PatientRegistrationRequest(
            @NotBlank String firstName,
            @NotBlank String lastName,
            String gender,
            LocalDate dateOfBirth,
            @NotBlank String phoneNumber,
            String email,
            String emergencyContact,
            String insuranceProvider,
            String policyNumber
    ) {}

    public record PatientResponse(
            Long id,
            String firstName,
            String lastName,
            String gender,
            LocalDate dateOfBirth,
            String phoneNumber,
            String email,
            String emergencyContact,
            String policyNumber,
            boolean degraded,
            java.time.OffsetDateTime lastUpdated
    ) {}

    public record TriageLogRequest(
            String bloodPressure,
            Double temperatureCelsius,
            Integer pulseRate,
            Double weightKg
    ) {}

    public record TriageResponse(
            Long id,
            Long patientId,
            String bloodPressure,
            Double temperatureCelsius,
            Integer pulseRate,
            Double weightKg,
            LocalDateTime recordedAt,
            Long recordedById,
            String recordedByName
    ) {}
}

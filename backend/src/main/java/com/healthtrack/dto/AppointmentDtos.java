package com.healthtrack.dto;

import com.healthtrack.entity.AppointmentStatus;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public class AppointmentDtos {

    public record DoctorResponse(
            Long id,
            Long userId,
            String fullName,
            String specialization,
            BigDecimal consultationFee,
            Boolean isAvailable
    ) {}

    public record AppointmentRequest(
            @NotNull Long patientId,
            @NotNull Long doctorId,
            @NotNull LocalDate appointmentDate,
            @NotNull LocalTime startTime,
            @NotNull LocalTime endTime,
            Boolean emergencyOverride
    ) {}

    public record AppointmentResponse(
            Long id,
            Long patientId,
            String patientName,
            Long doctorId,
            String doctorName,
            LocalDate appointmentDate,
            LocalTime startTime,
            LocalTime endTime,
            AppointmentStatus status,
            Integer tokenNumber
    ) {}

    public record StatusUpdateRequest(
            @NotNull AppointmentStatus status
    ) {}

    public record QueueEntry(
            Long appointmentId,
            Integer tokenNumber,
            Long patientId,
            String patientName,
            AppointmentStatus status,
            LocalTime startTime
    ) {}
}

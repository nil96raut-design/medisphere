package com.healthtrack.dto;

import com.healthtrack.entity.AppointmentStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class DoctorDtos {

    public record TodayScheduleResponse(
            Long appointmentId,
            Long patientId,
            String patientName,
            Integer age,
            String gender,
            String phoneNumber,
            LocalDate appointmentDate,
            LocalTime startTime,
            LocalTime endTime,
            AppointmentStatus status,
            Integer tokenNumber,
            boolean degraded,
            java.time.OffsetDateTime lastUpdated
    ) {}

    public record DoctorStatsResponse(
            long patientsToday,
            long completedConsultations,
            long pendingConsultations,
            long totalPrescriptionsToday,
            long labOrdersToday
    ) {}

    public record PatientFullProfileResponse(
            PatientDtos.PatientResponse patient,
            List<AppointmentDtos.AppointmentResponse> appointments,
            List<EmrDtos.MedicalRecordResponse> medicalRecords,
            List<LabDtos.LabOrderResponse> labOrders
    ) {}
}

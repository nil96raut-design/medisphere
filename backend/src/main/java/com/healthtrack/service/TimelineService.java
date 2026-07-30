package com.healthtrack.service;

import com.healthtrack.dto.TimelineEventDto;
import com.healthtrack.entity.*;
import com.healthtrack.repository.*;
import com.healthtrack.security.TenantValidator;
import com.healthtrack.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TimelineService {

    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final VitalRecordRepository vitalRecordRepository;
    private final LabTestOrderRepository labTestOrderRepository;
    private final BillRepository billRepository;
    private final DispensationRecordRepository dispensationRecordRepository;
    private final TenantValidator tenantValidator;

    @Transactional(readOnly = true)
    public List<TimelineEventDto> getPatientTimeline(Long patientId, UserPrincipal currentUser) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"));
        tenantValidator.validateHospitalAccess(patient.getHospital().getId(), currentUser.getHospitalId());

        List<TimelineEventDto> events = new ArrayList<>();

        // 1. Appointments
        List<Appointment> appointments = appointmentRepository.findByPatientId(patientId);
        for (Appointment a : appointments) {
            OffsetDateTime timestamp = (a.getAppointmentDate() != null && a.getStartTime() != null) ?
                    a.getAppointmentDate().atTime(a.getStartTime()).atOffset(ZoneOffset.UTC) :
                    OffsetDateTime.now();

            String docName = (a.getDoctor() != null && a.getDoctor().getUser() != null) ?
                    a.getDoctor().getUser().getFullName() : "Doctor";

            Map<String, Object> meta = new HashMap<>();
            meta.put("status", a.getStatus() != null ? a.getStatus().name() : "N/A");
            meta.put("doctor", docName);
            meta.put("tokenNumber", a.getTokenNumber());

            events.add(new TimelineEventDto(
                    a.getId(),
                    "APPOINTMENT",
                    "Appointment with " + docName,
                    "Status: " + a.getStatus() + " | Token: " + (a.getTokenNumber() != null ? a.getTokenNumber() : "N/A"),
                    timestamp,
                    meta
            ));
        }

        // 2. Vitals
        List<VitalRecord> vitals = vitalRecordRepository.findByPatientIdOrderByRecordedAtDesc(patientId);
        for (VitalRecord v : vitals) {
            Map<String, Object> meta = new HashMap<>();
            meta.put("bloodPressure", v.getBloodPressure());
            meta.put("heartRate", v.getHeartRate());
            meta.put("temperature", v.getTemperature());
            meta.put("spo2", v.getSpo2());
            meta.put("alertFlag", v.getAlertFlag());

            events.add(new TimelineEventDto(
                    v.getId(),
                    "VITALS",
                    "Vitals Recorded" + (Boolean.TRUE.equals(v.getAlertFlag()) ? " (ALERT)" : ""),
                    String.format("BP: %s, HR: %s, Temp: %s, SpO2: %s",
                            v.getBloodPressure(), v.getHeartRate(), v.getTemperature(), v.getSpo2()),
                    v.getRecordedAt() != null ? v.getRecordedAt() : OffsetDateTime.now(),
                    meta
            ));
        }

        // 3. Lab Test Orders
        List<LabTestOrder> labs = labTestOrderRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
        for (LabTestOrder l : labs) {
            Map<String, Object> meta = new HashMap<>();
            meta.put("status", l.getStatus() != null ? l.getStatus().name() : "N/A");
            meta.put("criticalFlag", l.getCriticalFlag());

            OffsetDateTime timestamp;
            if (l.getCreatedAt() != null) {
                timestamp = l.getCreatedAt().atOffset(ZoneOffset.UTC);
            } else {
                timestamp = OffsetDateTime.now();
            }

            events.add(new TimelineEventDto(
                    l.getId(),
                    "LAB_TEST",
                    "Lab Order: " + l.getTestName(),
                    "Status: " + l.getStatus() + (l.getResultValues() != null ? " | Results: " + l.getResultValues() : ""),
                    timestamp,
                    meta
            ));
        }

        // 4. Bills
        List<Bill> bills = billRepository.findByPatientId(patientId);
        for (Bill b : bills) {
            Map<String, Object> meta = new HashMap<>();
            meta.put("netPayable", b.getNetPayable());
            meta.put("paymentStatus", b.getPaymentStatus() != null ? b.getPaymentStatus().name() : "N/A");

            events.add(new TimelineEventDto(
                    b.getId(),
                    "BILLING",
                    "Bill Generated - $" + b.getNetPayable(),
                    "Status: " + b.getPaymentStatus() + " | Mode: " + b.getPaymentMode(),
                    b.getCreatedAt() != null ? b.getCreatedAt() : OffsetDateTime.now(),
                    meta
            ));
        }

        // 5. Pharmacy Dispensations
        List<DispensationRecord> dispensations = dispensationRecordRepository.findByPatientIdOrderByDispensedAtDesc(patientId);
        for (DispensationRecord d : dispensations) {
            OffsetDateTime timestamp;
            if (d.getDispensedAt() != null) {
                timestamp = d.getDispensedAt().atOffset(ZoneOffset.UTC);
            } else {
                timestamp = OffsetDateTime.now();
            }

            Map<String, Object> meta = new HashMap<>();
            meta.put("quantity", d.getQuantityDispensed());
            meta.put("unitPrice", d.getUnitPrice());
            meta.put("totalPrice", d.getTotalPrice());

            events.add(new TimelineEventDto(
                    d.getId(),
                    "PHARMACY",
                    "Medication Dispensed: " + d.getMedicineName(),
                    "Qty: " + d.getQuantityDispensed() + " | Total: $" + d.getTotalPrice(),
                    timestamp,
                    meta
            ));
        }

        // Sort descending chronologically
        events.sort(Comparator.comparing(TimelineEventDto::timestamp).reversed());
        return events;
    }
}

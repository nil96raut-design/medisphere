package com.healthtrack.service;

import com.healthtrack.dto.AppointmentDtos.AppointmentResponse;
import com.healthtrack.dto.AppointmentDtos.DoctorResponse;
import com.healthtrack.dto.DoctorDtos.DoctorStatsResponse;
import com.healthtrack.dto.DoctorDtos.PatientFullProfileResponse;
import com.healthtrack.dto.DoctorDtos.TodayScheduleResponse;
import com.healthtrack.dto.EmrDtos.MedicalRecordResponse;
import com.healthtrack.dto.LabDtos.LabOrderResponse;
import com.healthtrack.dto.PatientDtos.PatientResponse;
import com.healthtrack.entity.*;
import com.healthtrack.repository.*;
import com.healthtrack.security.TenantValidator;
import com.healthtrack.security.UserPrincipal;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final LabTestOrderRepository labTestOrderRepository;
    private final TenantValidator tenantValidator;
    private final MeterRegistry meterRegistry;

    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "database", fallbackMethod = "fallbackGetAvailableDoctors")
    @Transactional(readOnly = true)
    public List<DoctorResponse> getAvailableDoctors(UserPrincipal currentUser) {
        requireDoctorRole(currentUser);
        return doctorRepository.findAvailableDoctors().stream()
                .map(d -> new DoctorResponse(
                        d.getId(), d.getUser().getId(), d.getUser().getFullName(),
                        d.getSpecialization(), d.getConsultationFee(), d.getIsAvailable()))
                .toList();
    }

    public List<DoctorResponse> fallbackGetAvailableDoctors(UserPrincipal currentUser, Throwable t) {
        meterRegistry.counter("fallback.invoked.count", "service", "DoctorService", "method", "getAvailableDoctors").increment();
        // Fallback: return empty list during degraded mode
        return List.of();
    }

    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "database", fallbackMethod = "fallbackGetTodaySchedule")
    @Transactional(readOnly = true)
    public List<TodayScheduleResponse> getTodaySchedule(UserPrincipal currentUser) {
        User doctorUser = currentUser.getUser();
        Doctor doctor = doctorRepository.findByUserId(doctorUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor profile not found"));

        LocalDate today = LocalDate.now();
        List<Appointment> appointments = appointmentRepository
                .findByDoctorIdAndAppointmentDateAndStatusNotOrderByTokenNumberAsc(
                        doctor.getId(), today, AppointmentStatus.CANCELLED);

        return appointments.stream().map(a -> {
            Patient p = a.getPatient();
            int age = p.getDateOfBirth() != null
                    ? today.getYear() - p.getDateOfBirth().getYear()
                    : 0;
            return new TodayScheduleResponse(
                    a.getId(), p.getId(),
                    p.getFirstName() + " " + p.getLastName(),
                    age, p.getGender(), p.getPhoneNumber(),
                    a.getAppointmentDate(), a.getStartTime(), a.getEndTime(),
                    a.getStatus(), a.getTokenNumber(), false, java.time.OffsetDateTime.now());
        }).toList();
    }

    public List<TodayScheduleResponse> fallbackGetTodaySchedule(UserPrincipal currentUser, Throwable t) {
        meterRegistry.counter("fallback.invoked.count", "service", "DoctorService", "method", "getTodaySchedule").increment();
        // Fallback: return empty schedule when DB is down, but include a degraded marker object
        return List.of(new TodayScheduleResponse(
                null, null, "Degraded Mode - Data Unavailable", null, null, null,
                LocalDate.now(), null, null, null, null, true, java.time.OffsetDateTime.now()
        ));
    }

    @Transactional(readOnly = true)
    public DoctorStatsResponse getStats(UserPrincipal currentUser) {
        User doctorUser = currentUser.getUser();
        Doctor doctor = doctorRepository.findByUserId(doctorUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor profile not found"));

        LocalDate today = LocalDate.now();
        List<Appointment> todayAppts = appointmentRepository
                .findByDoctorIdAndAppointmentDateAndStatusNotOrderByTokenNumberAsc(
                        doctor.getId(), today, AppointmentStatus.CANCELLED);

        long total = todayAppts.size();
        long completed = todayAppts.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED).count();
        long pending = total - completed;

        long prescriptions = medicalRecordRepository.countByDoctorIdAndEncounterDate(doctorUser.getId(), today);
        long labOrders = labTestOrderRepository.countByRequestedByAndCreatedAtBetween(
                doctorUser, today.atStartOfDay(), today.plusDays(1).atStartOfDay());

        return new DoctorStatsResponse(total, completed, pending, prescriptions, labOrders);
    }

    @Transactional(readOnly = true)
    public PatientFullProfileResponse getPatientFullProfile(Long patientId, UserPrincipal currentUser) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"));
        tenantValidator.validateHospitalAccess(patient.getHospital().getId(), currentUser.getHospitalId());

        PatientResponse patientResp = new PatientResponse(
                patient.getId(), patient.getFirstName(), patient.getLastName(),
                patient.getGender(), patient.getDateOfBirth(), patient.getPhoneNumber(),
                patient.getEmail(), patient.getEmergencyContact(),
                patient.getPolicyNumber(), false, java.time.OffsetDateTime.now());

        List<Appointment> appointments = appointmentRepository.findByPatientIdWithDoctor(patientId);
        List<AppointmentResponse> apptResponses = appointments.stream()
                .map(a -> new AppointmentResponse(
                        a.getId(), a.getPatient().getId(),
                        a.getPatient().getFirstName() + " " + a.getPatient().getLastName(),
                        a.getDoctor().getId(), a.getDoctor().getUser().getFullName(),
                        a.getAppointmentDate(), a.getStartTime(), a.getEndTime(),
                        a.getStatus(), a.getTokenNumber()))
                .toList();

        List<MedicalRecord> records = medicalRecordRepository.findByPatientIdOrderByEncounterDateDesc(patientId);
        List<MedicalRecordResponse> recordResponses = records.stream()
                .map(r -> {
                    var prescriptions = r.getPrescriptions().stream()
                            .map(p -> new com.healthtrack.dto.EmrDtos.PrescriptionItemResponse(
                                    p.getId(), p.getMedicineName(), p.getDosage(),
                                    p.getFrequency(), p.getDuration(), p.getInstructions()))
                            .toList();
                    var services = r.getServiceRequests().stream()
                            .map(s -> new com.healthtrack.dto.EmrDtos.ServiceRequestResponse(
                                    s.getId(), s.getServiceType().name(),
                                    s.getServiceDetails(), s.getStatus().name()))
                            .toList();
                    return new MedicalRecordResponse(
                            r.getId(), r.getPatient().getId(),
                            r.getPatient().getFirstName() + " " + r.getPatient().getLastName(),
                            r.getDoctor().getId(), r.getDoctor().getFullName(),
                            r.getEncounterDate(), r.getChiefComplaints(),
                            r.getObjectiveFindings(), r.getDiagnosis(),
                            r.getNextFollowUpDate(), prescriptions, services);
                })
                .toList();

        List<LabTestOrder> labOrders = labTestOrderRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
        List<LabOrderResponse> labResponses = labOrders.stream()
                .map(o -> new LabOrderResponse(
                        o.getId(), o.getPatient().getId(),
                        o.getPatient().getFirstName() + " " + o.getPatient().getLastName(),
                        o.getTestName(),
                        o.getRequestedBy() != null ? o.getRequestedBy().getFullName() : null,
                        o.getStatus().name(), o.getResultValues(), o.getTechnicianNotes(),
                        o.getCompletedAt(), o.getCreatedAt(), o.getSampleCollectedAt(),
                        o.getProcessingStartedAt(), o.getProcessingCompletedAt(),
                        o.getSampleBarcode(), o.getSampleStorageLocation(),
                        o.getCriticalFlag(), o.getTurnaroundMinutes(),
                        o.getRetestOf() != null ? o.getRetestOf().getId() : null,
                        o.getCorrectionReason(),
                        o.getResultEnteredBy() != null ? o.getResultEnteredBy().getFullName() : null,
                        o.getPrice(), List.of()))
                .toList();

        return new PatientFullProfileResponse(patientResp, apptResponses, recordResponses, labResponses);
    }

    private void requireDoctorRole(UserPrincipal currentUser) {
        Role role = currentUser.getUser().getRole();
        if (role != Role.ADMIN && role != Role.DOCTOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only Doctors or Admin can access this");
        }
    }
}

package com.healthtrack.service;

import com.healthtrack.dto.AppointmentDtos.*;
import com.healthtrack.entity.*;
import com.healthtrack.repository.AppointmentRepository;
import com.healthtrack.repository.DoctorRepository;
import com.healthtrack.repository.PatientRepository;
import com.healthtrack.security.TenantValidator;
import com.healthtrack.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private static final Map<AppointmentStatus, Set<AppointmentStatus>> ALLOWED_TRANSITIONS = Map.of(
            AppointmentStatus.SCHEDULED, Set.of(AppointmentStatus.CHECKED_IN, AppointmentStatus.CANCELLED, AppointmentStatus.COMPLETED, AppointmentStatus.NO_SHOW),
            AppointmentStatus.CHECKED_IN, Set.of(AppointmentStatus.IN_CONSULTATION, AppointmentStatus.CANCELLED, AppointmentStatus.NO_SHOW, AppointmentStatus.COMPLETED),
            AppointmentStatus.IN_CONSULTATION, Set.of(AppointmentStatus.COMPLETED, AppointmentStatus.CANCELLED),
            AppointmentStatus.COMPLETED, Set.of(),
            AppointmentStatus.CANCELLED, Set.of(),
            AppointmentStatus.NO_SHOW, Set.of()
    );

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final TenantValidator tenantValidator;

    @Transactional
    public AppointmentResponse bookAppointment(AppointmentRequest request, UserPrincipal currentUser) {
        requireBookingRole(currentUser);

        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"));
        tenantValidator.validateHospitalAccess(patient.getHospital().getId(), currentUser.getHospitalId());

        Doctor doctor = doctorRepository.findByIdLocked(request.doctorId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor not found"));

        List<Appointment> overlapping = appointmentRepository.findOverlappingLocked(
                request.doctorId(), request.appointmentDate(),
                request.startTime(), request.endTime());

        if (!overlapping.isEmpty() && !Boolean.TRUE.equals(request.emergencyOverride())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Doctor is already booked during this time slot");
        }

        Appointment appointment = Appointment.builder()
                .hospital(doctor.getHospital())
                .patient(patient)
                .doctor(doctor)
                .appointmentDate(request.appointmentDate())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .status(AppointmentStatus.SCHEDULED)
                .build();

        appointment = appointmentRepository.save(appointment);
        return mapToResponse(appointment);
    }

    @Transactional
    public AppointmentResponse updateStatus(Long appointmentId, StatusUpdateRequest request, UserPrincipal currentUser) {
        requireBookingRole(currentUser);

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));
        tenantValidator.validateHospitalAccess(appointment.getHospital().getId(), currentUser.getHospitalId());

        AppointmentStatus current = appointment.getStatus();
        AppointmentStatus target = request.status();

        Set<AppointmentStatus> allowed = ALLOWED_TRANSITIONS.get(current);
        if (allowed == null || !allowed.contains(target)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot transition from " + current + " to " + target);
        }

        if (target == AppointmentStatus.CHECKED_IN) {
            List<Appointment> lastTokens = appointmentRepository.findLastTokenLocked(
                    appointment.getDoctor().getId(), appointment.getAppointmentDate());

            int nextToken = lastTokens.stream()
                    .filter(a -> a.getTokenNumber() != null)
                    .mapToInt(Appointment::getTokenNumber)
                    .max()
                    .orElse(0) + 1;

            appointment.setTokenNumber(nextToken);
        }

        appointment.setStatus(target);
        appointment = appointmentRepository.save(appointment);
        return mapToResponse(appointment);
    }

    @Transactional(readOnly = true)
    public List<QueueEntry> getQueue(Long doctorId, UserPrincipal currentUser) {
        requireBookingRole(currentUser);

        List<Appointment> appointments = appointmentRepository
                .findByDoctorIdAndAppointmentDateAndStatusNotOrderByTokenNumberAsc(
                        doctorId, java.time.LocalDate.now(), AppointmentStatus.CANCELLED);

        return appointments.stream()
                .map(a -> new QueueEntry(
                        a.getId(),
                        a.getTokenNumber(),
                        a.getPatient().getId(),
                        a.getPatient().getFirstName() + " " + a.getPatient().getLastName(),
                        a.getStatus(),
                        a.getStartTime()))
                .toList();
    }

    @Transactional(readOnly = true)
    public AppointmentResponse getAppointment(Long appointmentId, UserPrincipal currentUser) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));
        tenantValidator.validateHospitalAccess(appointment.getHospital().getId(), currentUser.getHospitalId());
        return mapToResponse(appointment);
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getDoctorSchedule(Long doctorId, LocalDate date, UserPrincipal currentUser) {
        tenantValidator.validateHospitalAccess(
                doctorRepository.findById(doctorId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor not found"))
                        .getHospital().getId(),
                currentUser.getHospitalId());

        List<Appointment> appointments = appointmentRepository
                .findByDoctorIdAndAppointmentDateAndStatusNotOrderByTokenNumberAsc(
                        doctorId, date, AppointmentStatus.CANCELLED);
        return appointments.stream().map(this::mapToResponse).toList();
    }

    private void requireBookingRole(UserPrincipal currentUser) {
        Role role = currentUser.getUser().getRole();
        if (role != Role.RECEPTIONIST && role != Role.ADMIN && role != Role.DOCTOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only Staff, Admin, or Doctor can manage appointments/queue");
        }
    }

    private AppointmentResponse mapToResponse(Appointment a) {
        return new AppointmentResponse(
                a.getId(),
                a.getPatient().getId(),
                a.getPatient().getFirstName() + " " + a.getPatient().getLastName(),
                a.getDoctor().getId(),
                a.getDoctor().getUser().getFullName(),
                a.getAppointmentDate(),
                a.getStartTime(),
                a.getEndTime(),
                a.getStatus(),
                a.getTokenNumber());
    }
}

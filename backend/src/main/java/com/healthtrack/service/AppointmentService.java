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

import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final TenantValidator tenantValidator;

    @Transactional
    public AppointmentResponse bookAppointment(AppointmentRequest request, UserPrincipal currentUser) {
        requireBookingRole(currentUser);

        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"));
        tenantValidator.validateHospitalAccess(patient.getHospital().getId(), currentUser.getHospitalId());

        // Fetch doctor and immediately lock the Doctor row with PESSIMISTIC_WRITE.
        // This serializes ALL concurrent booking attempts for this specific doctor
        // at the database level — even when no Appointment row exists yet for the
        // requested slot (which is the case the old code missed). Locking the parent
        // resource (Doctor) rather than the child rows (Appointment) guarantees there
        // is always a row to lock, so the second transaction blocks until the first
        // commits and then sees the newly inserted overlapping Appointment.
        // The lock is scoped to a single Doctor row (by primary key), so bookings for
        // different doctors proceed in parallel without contention.
        Doctor doctor = doctorRepository.findByIdLocked(request.doctorId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor not found"));

        // With the Doctor row locked, check for overlapping appointments.
        // If a prior transaction just committed an overlapping slot, we see it now.
        // No other transaction can be simultaneously checking or inserting for this
        // doctor because they are blocked on the Doctor row lock above.
        List<Appointment> overlapping = appointmentRepository.findOverlappingLocked(
                request.doctorId(), request.appointmentDate(),
                request.startTime(), request.endTime());

        if (!overlapping.isEmpty()) {
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

        if (request.status() == AppointmentStatus.CHECKED_IN) {
            if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Only SCHEDULED appointments can be checked in");
            }

            // Token number generation: use PESSIMISTIC_WRITE to lock the doctor's
            // non-cancelled appointments for the day, then compute MAX+1. The
            // exclusive lock serializes concurrent check-ins, guaranteeing
            // unique sequential tokens per doctor per day.
            List<Appointment> lastTokens = appointmentRepository.findLastTokenLocked(
                    appointment.getDoctor().getId(), appointment.getAppointmentDate());

            int nextToken = lastTokens.stream()
                    .filter(a -> a.getTokenNumber() != null)
                    .mapToInt(Appointment::getTokenNumber)
                    .max()
                    .orElse(0) + 1;

            appointment.setTokenNumber(nextToken);
        }

        appointment.setStatus(request.status());
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

    private void requireBookingRole(UserPrincipal currentUser) {
        Role role = currentUser.getUser().getRole();
        // NOTE: RECEPTIONIST role does not exist in the Role enum.
        // Using RECEPTIONIST and ADMIN as the closest equivalents.
        // If a dedicated RECEPTIONIST role is desired, it must be added to Role.java.
        if (role != Role.RECEPTIONIST && role != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only Staff or Admin can manage appointments/queue");
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

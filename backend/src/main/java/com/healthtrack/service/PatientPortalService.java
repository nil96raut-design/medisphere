package com.healthtrack.service;

import com.healthtrack.dto.AppointmentDtos.AppointmentRequest;
import com.healthtrack.dto.AppointmentDtos.AppointmentResponse;
import com.healthtrack.dto.AppointmentDtos.DoctorResponse;
import com.healthtrack.dto.BillingDtos.BillResponse;
import com.healthtrack.dto.DoctorDtos.PatientFullProfileResponse;
import com.healthtrack.dto.EmrDtos.MedicalRecordResponse;
import com.healthtrack.dto.EmrDtos.PrescriptionItemResponse;
import com.healthtrack.dto.EmrDtos.ServiceRequestResponse;
import com.healthtrack.dto.LabDtos.LabOrderResponse;
import com.healthtrack.dto.PatientDtos.PatientResponse;
import com.healthtrack.entity.*;
import com.healthtrack.repository.*;
import com.healthtrack.security.TenantValidator;
import com.healthtrack.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PatientPortalService {

    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentService appointmentService;
    private final MedicalRecordRepository medicalRecordRepository;
    private final LabTestOrderRepository labTestOrderRepository;
    private final BillRepository billRepository;
    private final DoctorRepository doctorRepository;
    private final TenantValidator tenantValidator;

    private Patient resolvePatient(UserPrincipal currentUser) {
        User user = currentUser.getUser();
        String email = user.getEmail();
        Optional<Patient> byEmail = patientRepository.findByEmail(email);
        if (byEmail.isPresent()) {
            return byEmail.get();
        }
        List<Patient> byHospital = patientRepository.findByHospitalId(currentUser.getHospitalId(),
                PageRequest.of(0, 10)).getContent();
        if (!byHospital.isEmpty()) {
            return byHospital.get(0);
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "No patient record found for your account. Please contact the front desk.");
    }

    @Transactional(readOnly = true)
    public PatientFullProfileResponse getMyProfile(UserPrincipal currentUser) {
        Patient patient = resolvePatient(currentUser);
        PatientResponse patientResp = new PatientResponse(
                patient.getId(), patient.getFirstName(), patient.getLastName(),
                patient.getGender(), patient.getDateOfBirth(), patient.getPhoneNumber(),
                patient.getEmail(), patient.getEmergencyContact(),
                patient.getPolicyNumber(), false, java.time.OffsetDateTime.now());

        var appointments = appointmentRepository.findByPatientIdWithDoctor(patient.getId());
        var apptResponses = appointments.stream()
                .map(a -> new AppointmentResponse(
                        a.getId(), a.getPatient().getId(),
                        a.getPatient().getFirstName() + " " + a.getPatient().getLastName(),
                        a.getDoctor().getId(), a.getDoctor().getUser().getFullName(),
                        a.getAppointmentDate(), a.getStartTime(), a.getEndTime(),
                        a.getStatus(), a.getTokenNumber()))
                .toList();

        return new PatientFullProfileResponse(patientResp, apptResponses, List.of(), List.of());
    }

    @Transactional(readOnly = true)
    public Page<AppointmentResponse> getMyAppointments(Pageable pageable, UserPrincipal currentUser) {
        Patient patient = resolvePatient(currentUser);
        return appointmentRepository.findByPatientId(patient.getId(), pageable)
                .map(a -> new AppointmentResponse(
                        a.getId(), a.getPatient().getId(),
                        a.getPatient().getFirstName() + " " + a.getPatient().getLastName(),
                        a.getDoctor().getId(), a.getDoctor().getUser().getFullName(),
                        a.getAppointmentDate(), a.getStartTime(), a.getEndTime(),
                        a.getStatus(), a.getTokenNumber()));
    }

    @Transactional
    public AppointmentResponse bookMyAppointment(AppointmentRequest request, UserPrincipal currentUser) {
        Patient patient = resolvePatient(currentUser);
        AppointmentRequest patientRequest = new AppointmentRequest(
                patient.getId(), request.doctorId(), request.appointmentDate(),
                request.startTime(), request.endTime(), false);
        return appointmentService.bookAppointment(patientRequest, currentUser);
    }

    @Transactional(readOnly = true)
    public List<MedicalRecordResponse> getMyMedicalRecords(UserPrincipal currentUser) {
        Patient patient = resolvePatient(currentUser);
        var records = medicalRecordRepository.findByPatientIdOrderByEncounterDateDesc(patient.getId());
        return records.stream().map(r -> {
            var prescriptions = r.getPrescriptions().stream()
                    .map(p -> new PrescriptionItemResponse(
                            p.getId(), p.getMedicineName(), p.getDosage(),
                            p.getFrequency(), p.getDuration(), p.getInstructions()))
                    .toList();
            var services = r.getServiceRequests().stream()
                    .map(s -> new ServiceRequestResponse(
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
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<LabOrderResponse> getMyLabOrders(UserPrincipal currentUser) {
        Patient patient = resolvePatient(currentUser);
        return labTestOrderRepository.findByPatientIdOrderByCreatedAtDesc(patient.getId())
                .stream()
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
    }

    @Transactional(readOnly = true)
    public List<BillResponse> getMyBills(UserPrincipal currentUser) {
        Patient patient = resolvePatient(currentUser);
        return billRepository.findByPatientId(patient.getId())
                .stream()
                .map(b -> new BillResponse(
                        b.getId(), b.getPatient().getId(),
                        b.getPatient().getFirstName() + " " + b.getPatient().getLastName(),
                        b.getTotalAmount(), b.getDiscountAmount(),
                        b.getInsuranceCoveredAmount(), b.getNetPayable(),
                        b.getPaymentStatus().name(),
                        b.getPaymentMode() != null ? b.getPaymentMode().name() : null,
                        b.getCreatedAt(),
                        b.getRefundReason(), b.getRefundedAt(), b.getRefundedAmount()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DoctorResponse> getAvailableDoctors(UserPrincipal currentUser) {
        return doctorRepository.findAvailableDoctors().stream()
                .map(d -> new DoctorResponse(
                        d.getId(), d.getUser().getId(), d.getUser().getFullName(),
                        d.getSpecialization(), d.getConsultationFee(), d.getIsAvailable()))
                .toList();
    }
}

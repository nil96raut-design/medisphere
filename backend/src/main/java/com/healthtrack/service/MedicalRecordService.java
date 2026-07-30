package com.healthtrack.service;

import com.healthtrack.dto.EmrDtos.*;
import com.healthtrack.entity.*;
import com.healthtrack.repository.MedicalRecordRepository;
import com.healthtrack.repository.PatientRepository;
import com.healthtrack.security.TenantValidator;
import com.healthtrack.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MedicalRecordService {

    private static final Logger log = LoggerFactory.getLogger(MedicalRecordService.class);

    private final MedicalRecordRepository medicalRecordRepository;
    private final PatientRepository patientRepository;
    private final com.healthtrack.repository.LabTestOrderRepository labTestOrderRepository;
    private final TenantValidator tenantValidator;
    private final MedicationSchedulingService medicationSchedulingService;

    @Transactional
    public MedicalRecordResponse createRecord(CreateMedicalRecordRequest request, UserPrincipal currentUser) {
        User doctor = currentUser.getUser();
        if (doctor.getRole() != Role.DOCTOR && doctor.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only doctors can create medical records");
        }

        Long hospitalId = currentUser.getHospitalId();
        if (hospitalId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not associated with a hospital");
        }

        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"));
        tenantValidator.validateHospitalAccess(patient.getHospital().getId(), currentUser.getHospitalId());

        Hospital hospital = patient.getHospital();

        MedicalRecord record = MedicalRecord.builder()
                .hospital(hospital)
                .patient(patient)
                .doctor(doctor)
                .encounterDate(request.encounterDate())
                .chiefComplaints(request.chiefComplaints())
                .objectiveFindings(request.objectiveFindings())
                .diagnosis(request.diagnosis())
                .nextFollowUpDate(request.nextFollowUpDate())
                .build();

        if (request.prescriptions() != null) {
            for (PrescriptionItemRequest p : request.prescriptions()) {
                PrescriptionItem item = PrescriptionItem.builder()
                        .medicalRecord(record)
                        .hospital(hospital)
                        .medicineName(p.medicineName())
                        .dosage(p.dosage())
                        .frequency(p.frequency())
                        .duration(p.duration())
                        .instructions(p.instructions())
                        .build();
                record.getPrescriptions().add(item);
            }
        }

        if (request.serviceRequests() != null) {
            for (ServiceRequestEntry s : request.serviceRequests()) {
                ServiceRequest sr = ServiceRequest.builder()
                        .medicalRecord(record)
                        .hospital(hospital)
                        .serviceType(ServiceType.valueOf(s.serviceType()))
                        .serviceDetails(s.serviceDetails())
                        .status(RequestStatus.PENDING)
                        .build();
                record.getServiceRequests().add(sr);

                if (sr.getServiceType() == ServiceType.LAB_TEST) {
                    labTestOrderRepository.save(LabTestOrder.builder()
                            .hospital(hospital)
                            .patient(patient)
                            .medicalRecord(record)
                            .testName(sr.getServiceDetails())
                            .requestedBy(doctor)
                            .status(LabOrderStatus.ORDERED)
                            .price(new java.math.BigDecimal("100.00")) // default price
                            .build());
                }
            }
        }

        record = medicalRecordRepository.save(record);

        for (PrescriptionItem item : record.getPrescriptions()) {
            try {
                medicationSchedulingService.generateSchedules(item.getId());
            } catch (Exception e) {
                log.warn("Failed to generate medication schedules for item {}", item.getId(), e);
            }
        }

        return mapToResponse(record);
    }

    @Transactional(readOnly = true)
    public List<MedicalRecordResponse> getPatientHistory(Long patientId, UserPrincipal currentUser) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"));
        tenantValidator.validateHospitalAccess(patient.getHospital().getId(), currentUser.getHospitalId());

        List<MedicalRecord> records = medicalRecordRepository.findByPatientIdOrderByEncounterDateDesc(patientId);
        return records.stream().map(this::mapToResponse).toList();
    }

    private MedicalRecordResponse mapToResponse(MedicalRecord r) {
        List<PrescriptionItemResponse> prescriptions = r.getPrescriptions().stream()
                .map(p -> new PrescriptionItemResponse(
                        p.getId(), p.getMedicineName(), p.getDosage(),
                        p.getFrequency(), p.getDuration(), p.getInstructions()))
                .toList();

        List<ServiceRequestResponse> services = r.getServiceRequests().stream()
                .map(s -> new ServiceRequestResponse(
                        s.getId(), s.getServiceType().name(),
                        s.getServiceDetails(), s.getStatus().name()))
                .toList();

        return new MedicalRecordResponse(
                r.getId(),
                r.getPatient().getId(),
                r.getPatient().getFirstName() + " " + r.getPatient().getLastName(),
                r.getDoctor().getId(),
                r.getDoctor().getFullName(),
                r.getEncounterDate(),
                r.getChiefComplaints(),
                r.getObjectiveFindings(),
                r.getDiagnosis(),
                r.getNextFollowUpDate(),
                prescriptions,
                services);
    }
}

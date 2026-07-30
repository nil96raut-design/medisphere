package com.healthtrack.service;

import com.healthtrack.dto.IpdDtos.*;
import com.healthtrack.entity.*;
import com.healthtrack.repository.*;
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
public class IpdService {

    private final BedRepository bedRepository;
    private final AdmissionRepository admissionRepository;
    private final NursingLogRepository nursingLogRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final TenantValidator tenantValidator;
    private final BedCleaningService bedCleaningService;
    private final DischargeIntegrityValidator dischargeIntegrityValidator;

    @Transactional(readOnly = true)
    public List<BedResponse> getAvailableBeds(UserPrincipal currentUser) {
        return bedRepository.findByIsOccupiedFalse().stream()
                .map(b -> new BedResponse(b.getId(), b.getWardName(), b.getBedNumber(),
                        b.getChargePerDay(), b.getIsOccupied()))
                .toList();
    }

    @Transactional
    public AdmissionResponse admitPatient(AdmissionRequest request, UserPrincipal currentUser) {
        User user = currentUser.getUser();
        if (user.getRole() != Role.DOCTOR && user.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only doctors can admit patients");
        }

        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"));
        tenantValidator.validateHospitalAccess(patient.getHospital().getId(), currentUser.getHospitalId());

        Bed bed = bedRepository.findByIdLocked(request.bedId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bed not found"));

        if (bed.getIsOccupied()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bed is already occupied");
        }

        User doctor = userRepository.findById(request.doctorId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor not found"));
        tenantValidator.validateHospitalAccess(doctor.getHospital().getId(), currentUser.getHospitalId());

        if (doctor.getRole() != Role.DOCTOR) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected user is not a doctor");
        }

        bed.setIsOccupied(true);
        bedRepository.save(bed);

        Admission admission = Admission.builder()
                .hospital(patient.getHospital())
                .patient(patient)
                .doctor(doctor)
                .bed(bed)
                .admissionDate(request.admissionDate())
                .initialDiagnosis(request.initialDiagnosis())
                .status(AdmissionStatus.ADMITTED)
                .build();

        admission = admissionRepository.save(admission);
        return mapToResponse(admission);
    }

    @Transactional
    public NursingLogResponse addNursingLog(Long admissionId, NursingLogRequest request, UserPrincipal currentUser) {
        User nurse = currentUser.getUser();
        if (nurse.getRole() != Role.NURSE && nurse.getRole() != Role.DOCTOR && nurse.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only nurses and doctors can add nursing logs");
        }

        Admission admission = admissionRepository.findById(admissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admission not found"));
        tenantValidator.validateHospitalAccess(admission.getHospital().getId(), currentUser.getHospitalId());

        if (admission.getStatus() == AdmissionStatus.DISCHARGED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot add logs to a discharged patient");
        }

        NursingLog log = NursingLog.builder()
                .admission(admission)
                .hospital(admission.getHospital())
                .nurse(nurse)
                .vitalsRecorded(request.vitalsRecorded())
                .medicineAdministered(request.medicineAdministered())
                .nursingNotes(request.nursingNotes())
                .build();

        log = nursingLogRepository.save(log);
        return new NursingLogResponse(
                log.getId(), log.getAdmission().getId(),
                log.getNurse().getId(), log.getNurse().getFullName(),
                log.getVitalsRecorded(), log.getMedicineAdministered(),
                log.getNursingNotes(), log.getLoggedAt());
    }

    @Transactional
    public AdmissionResponse discharge(Long admissionId, DischargeRequest request, UserPrincipal currentUser) {
        User user = currentUser.getUser();
        if (user.getRole() != Role.DOCTOR && user.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only doctors can discharge patients");
        }

        Admission admission = admissionRepository.findById(admissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admission not found"));
        tenantValidator.validateHospitalAccess(admission.getHospital().getId(), currentUser.getHospitalId());

        if (admission.getStatus() == AdmissionStatus.DISCHARGED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Patient is already discharged");
        }

        dischargeIntegrityValidator.validateDischarge(admission);

        admission.setStatus(AdmissionStatus.DISCHARGED);
        admission.setDischargeDate(java.time.LocalDate.now());
        admission.setDischargeSummary(request.dischargeSummary());
        admission.getBed().setIsOccupied(false);
        admission = admissionRepository.save(admission);

        bedCleaningService.autoRequestCleaningOnDischarge(admission.getBed().getId(), currentUser);

        return mapToResponse(admission);
    }

    @Transactional(readOnly = true)
    public List<AdmissionResponse> getActiveAdmissions(UserPrincipal currentUser) {
        return admissionRepository.findByStatusOrderByAdmissionDateDesc(AdmissionStatus.ADMITTED).stream()
                .map(this::mapToResponse).toList();
    }

    private AdmissionResponse mapToResponse(Admission a) {
        return new AdmissionResponse(
                a.getId(),
                a.getPatient().getId(),
                a.getPatient().getFirstName() + " " + a.getPatient().getLastName(),
                a.getDoctor().getId(),
                a.getDoctor().getFullName(),
                a.getBed().getId(),
                a.getBed().getWardName(),
                a.getBed().getBedNumber(),
                a.getAdmissionDate(),
                a.getDischargeDate(),
                a.getInitialDiagnosis(),
                a.getDischargeSummary(),
                a.getStatus().name());
    }
}

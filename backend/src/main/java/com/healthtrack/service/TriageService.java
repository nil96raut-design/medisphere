package com.healthtrack.service;

import com.healthtrack.dto.PatientDtos.TriageLogRequest;
import com.healthtrack.dto.PatientDtos.TriageResponse;
import com.healthtrack.entity.Patient;
import com.healthtrack.entity.Role;
import com.healthtrack.entity.Triage;
import com.healthtrack.repository.PatientRepository;
import com.healthtrack.repository.TriageRepository;
import com.healthtrack.security.TenantValidator;
import com.healthtrack.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TriageService {

    private final TriageRepository triageRepository;
    private final PatientRepository patientRepository;
    private final TenantValidator tenantValidator;

    @Transactional
    public TriageResponse logVitals(Long patientId, TriageLogRequest request, UserPrincipal currentUser) {
        requireFrontDeskRole(currentUser);

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"));
        tenantValidator.validateHospitalAccess(patient.getHospital().getId(), currentUser.getHospitalId());

        Triage triage = Triage.builder()
                .patient(patient)
                .hospital(patient.getHospital())
                .bloodPressure(request.bloodPressure())
                .temperatureCelsius(request.temperatureCelsius())
                .pulseRate(request.pulseRate())
                .weightKg(request.weightKg())
                .recordedAt(LocalDateTime.now())
                .recordedBy(currentUser.getUser())
                .build();

        triage = triageRepository.save(triage);
        return mapToResponse(triage);
    }

    private void requireFrontDeskRole(UserPrincipal currentUser) {
        Role role = currentUser.getUser().getRole();
        if (role != Role.RECEPTIONIST && role != Role.ADMIN && role != Role.DOCTOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only Staff, Admin, or Doctor can log vitals");
        }
    }

    private TriageResponse mapToResponse(Triage triage) {
        return new TriageResponse(
                triage.getId(),
                triage.getPatient().getId(),
                triage.getBloodPressure(),
                triage.getTemperatureCelsius(),
                triage.getPulseRate(),
                triage.getWeightKg(),
                triage.getRecordedAt(),
                triage.getRecordedBy().getId(),
                triage.getRecordedBy().getFullName()
        );
    }
}

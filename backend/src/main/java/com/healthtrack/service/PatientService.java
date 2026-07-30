package com.healthtrack.service;

import com.healthtrack.dto.PatientDtos.*;
import com.healthtrack.entity.Hospital;
import com.healthtrack.entity.Patient;
import com.healthtrack.entity.Role;

import com.healthtrack.repository.HospitalRepository;
import com.healthtrack.repository.PatientRepository;
import com.healthtrack.security.UserPrincipal;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import io.micrometer.core.instrument.MeterRegistry;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;
    private final HospitalRepository hospitalRepository;
    private final MeterRegistry meterRegistry;

    @Transactional
    public PatientResponse registerPatient(PatientRegistrationRequest request, UserPrincipal currentUser) {
        requireFrontDeskRole(currentUser);

        Hospital hospital = hospitalRepository.findById(currentUser.getHospitalId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hospital not found"));

        Patient patient = Patient.builder()
                .hospital(hospital)
                .firstName(request.firstName())
                .lastName(request.lastName())
                .gender(request.gender())
                .dateOfBirth(request.dateOfBirth())
                .phoneNumber(request.phoneNumber())
                .email(request.email())
                .emergencyContact(request.emergencyContact())
                .insuranceProvider(request.insuranceProvider())
                .policyNumber(request.policyNumber())
                .build();

        patient = patientRepository.save(patient);
        return mapToResponse(patient);
    }

    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "database", fallbackMethod = "fallbackGetPatient")
    @Transactional(readOnly = true)
    public PatientResponse getPatient(Long id, UserPrincipal currentUser) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"));
        validateHospitalAccess(patient.getHospital().getId(), currentUser.getHospitalId());
        return mapToResponse(patient);
    }

    public PatientResponse fallbackGetPatient(Long id, UserPrincipal currentUser, Throwable t) {
        meterRegistry.counter("fallback.invoked.count", "service", "PatientService", "method", "getPatient").increment();
        return new PatientResponse(id, "Unavailable", "Due to System Degradation", null, null, null, null, null, null, true, java.time.OffsetDateTime.now());
    }

    @Transactional
    public PatientResponse updatePatient(Long id, PatientRegistrationRequest request, UserPrincipal currentUser) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"));
        validateHospitalAccess(patient.getHospital().getId(), currentUser.getHospitalId());

        patient.setFirstName(request.firstName());
        patient.setLastName(request.lastName());
        patient.setGender(request.gender());
        patient.setDateOfBirth(request.dateOfBirth());
        patient.setPhoneNumber(request.phoneNumber());
        patient.setEmail(request.email());
        patient.setEmergencyContact(request.emergencyContact());
        patient.setInsuranceProvider(request.insuranceProvider());
        patient.setPolicyNumber(request.policyNumber());

        patient = patientRepository.save(patient);
        return mapToResponse(patient);
    }

    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "database", fallbackMethod = "fallbackSearchPatients")
    @Transactional(readOnly = true)
    public Page<PatientResponse> searchPatients(String query, Pageable pageable, UserPrincipal currentUser) {
        requireFrontDeskRole(currentUser);
        Long hospitalId = currentUser.getHospitalId();
        
        Page<Patient> patients;
        if (query == null || query.trim().isEmpty()) {
            patients = patientRepository.findByHospitalId(hospitalId, pageable);
        } else {
            patients = patientRepository.searchPatients(query, hospitalId, pageable);
        }
        
        return patients.map(this::mapToResponse);
    }

    public Page<PatientResponse> fallbackSearchPatients(String query, Pageable pageable, UserPrincipal currentUser, Throwable t) {
        meterRegistry.counter("fallback.invoked.count", "service", "PatientService", "method", "searchPatients").increment();
        return org.springframework.data.domain.Page.empty(pageable);
    }

    private void validateHospitalAccess(Long entityHospitalId, Long currentHospitalId) {
        if (entityHospitalId != null && !entityHospitalId.equals(currentHospitalId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cross-tenant data access denied");
        }
    }

    private void requireFrontDeskRole(UserPrincipal currentUser) {
        Role role = currentUser.getUser().getRole();
        if (role != Role.RECEPTIONIST && role != Role.ADMIN && role != Role.DOCTOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only Staff, Admin, or Doctor can manage patients");
        }
    }

    private PatientResponse mapToResponse(Patient p) {
        return new PatientResponse(
                p.getId(),
                p.getFirstName(),
                p.getLastName(),
                p.getGender(),
                p.getDateOfBirth(),
                p.getPhoneNumber(),
                p.getEmail(),
                p.getInsuranceProvider(),
                p.getPolicyNumber(),
                true,
                p.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toOffsetDateTime()
        );
    }
}

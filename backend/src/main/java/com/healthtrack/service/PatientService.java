package com.healthtrack.service;

import com.healthtrack.dto.PatientDtos.*;
import com.healthtrack.entity.Hospital;
import com.healthtrack.entity.Patient;
import com.healthtrack.entity.Role;
import com.healthtrack.entity.User;
import com.healthtrack.repository.HospitalRepository;
import com.healthtrack.repository.PatientRepository;
import com.healthtrack.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;
    private final HospitalRepository hospitalRepository;

    @Transactional
    public PatientResponse registerPatient(PatientRegistrationRequest request, UserPrincipal currentUser) {
        requireFrontDeskRole(currentUser);

        Long hospitalId = currentUser.getHospitalId();
        if (hospitalId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not associated with a hospital");
        }

        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Hospital not found"));

        if (patientRepository.findByPhoneNumber(request.phoneNumber()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Patient with this phone number already exists in the system");
        }

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

    @Transactional(readOnly = true)
    public List<PatientResponse> searchPatients(String query, UserPrincipal currentUser) {
        requireFrontDeskRole(currentUser);
        
        List<Patient> patients;
        if (query == null || query.trim().isEmpty()) {
            patients = patientRepository.findAll();
        } else {
            patients = patientRepository.searchPatients(query);
        }
        
        return patients.stream().map(this::mapToResponse).toList();
    }

    private void requireFrontDeskRole(UserPrincipal currentUser) {
        Role role = currentUser.getUser().getRole();
        if (role != Role.RECEPTIONIST && role != Role.ADMIN && role != Role.DOCTOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only Staff, Admin, or Doctor can manage patients");
        }
    }

    private PatientResponse mapToResponse(Patient patient) {
        return new PatientResponse(
                patient.getId(),
                patient.getFirstName(),
                patient.getLastName(),
                patient.getGender(),
                patient.getDateOfBirth(),
                patient.getPhoneNumber(),
                patient.getEmail(),
                patient.getEmergencyContact(),
                patient.getInsuranceProvider(),
                patient.getPolicyNumber()
        );
    }
}

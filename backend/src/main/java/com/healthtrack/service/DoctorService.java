package com.healthtrack.service;

import com.healthtrack.dto.AppointmentDtos.DoctorResponse;
import com.healthtrack.entity.Role;
import com.healthtrack.repository.DoctorRepository;
import com.healthtrack.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DoctorService {

    private final DoctorRepository doctorRepository;

    @Transactional(readOnly = true)
    public List<DoctorResponse> getAvailableDoctors(UserPrincipal currentUser) {
        requireFrontDeskRole(currentUser);

        return doctorRepository.findAvailableDoctors().stream()
                .map(d -> new DoctorResponse(
                        d.getId(), d.getUser().getId(), d.getUser().getFullName(),
                        d.getSpecialization(), d.getConsultationFee(), d.getIsAvailable()))
                .toList();
    }

    private void requireFrontDeskRole(UserPrincipal currentUser) {
        Role role = currentUser.getUser().getRole();
        if (role != Role.RECEPTIONIST && role != Role.ADMIN && role != Role.DOCTOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only Staff, Admin, or Doctor can access this");
        }
    }
}

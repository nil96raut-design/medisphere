package com.healthtrack.controller;

import com.healthtrack.dto.UserDtos.*;
import com.healthtrack.entity.Hospital;
import com.healthtrack.entity.Role;
import com.healthtrack.entity.User;
import com.healthtrack.repository.UserRepository;
import com.healthtrack.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final com.healthtrack.repository.HospitalRepository hospitalRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/by-role")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'ADMIN', 'DOCTOR', 'NURSE', 'PHARMACIST', 'LAB_TECH')")
    public ResponseEntity<List<UserSummary>> byRole(@RequestParam Role role) {
        List<UserSummary> users = userRepository.findByRole(role).stream()
                .filter(u -> u.getHospital() != null)
                .map(u -> new UserSummary(u.getId(), u.getFullName(), u.getEmail(), u.getRole()))
                .toList();
        return ResponseEntity.ok(users);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserSummary> createUser(@RequestBody CreateUserRequest request,
                                                   @AuthenticationPrincipal UserPrincipal principal) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        Long hospitalId = principal.getHospitalId();
        Hospital hospital = hospitalId != null ? hospitalRepository.getReferenceById(hospitalId) : null;

        User newUser = User.builder()
                .fullName(request.fullName())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .hospital(hospital)
                .build();

        User saved = userRepository.save(newUser);
        return ResponseEntity.ok(new UserSummary(saved.getId(), saved.getFullName(), saved.getEmail(), saved.getRole()));
    }
}

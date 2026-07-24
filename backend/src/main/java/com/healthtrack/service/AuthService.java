package com.healthtrack.service;

import com.healthtrack.dto.AuthDtos.*;
import com.healthtrack.entity.Role;
import com.healthtrack.entity.User;
import com.healthtrack.entity.Hospital;
import com.healthtrack.entity.SubscriptionTier;
import com.healthtrack.entity.SubscriptionStatus;
import com.healthtrack.repository.UserRepository;
import com.healthtrack.repository.HospitalRepository;
import com.healthtrack.security.JwtService;
import com.healthtrack.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final HospitalRepository hospitalRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        Role role = request.role() == null ? Role.PATIENT : request.role();

        // For now, default public signups go to Hospital 1. 
        // In the future, this should be selected or determined via invitation.
        Hospital hospital = hospitalRepository.findById(1L)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Default hospital not found"));

        User.UserBuilder builder = User.builder()
                .fullName(request.fullName())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(role)
                .hospital(hospital)
                .allergies(request.allergies());

        if (role == Role.PATIENT && request.primaryDoctorId() != null) {
            User doctor = userRepository.findById(request.primaryDoctorId())
                    .filter(u -> u.getRole() == Role.DOCTOR)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid primaryDoctorId"));
            builder.primaryDoctor(doctor);
        }

        User saved = userRepository.save(builder.build());
        return buildAuthResponse(saved);
    }

    @Transactional
    public AuthResponse hospitalSignup(HospitalSignupRequest request) {
        if (hospitalRepository.existsByLicenseNumber(request.licenseNumber())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "License number already registered");
        }
        if (userRepository.existsByEmail(request.adminEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Admin email already registered");
        }

        Hospital hospital = Hospital.builder()
                .name(request.hospitalName())
                .licenseNumber(request.licenseNumber())
                .contactEmail(request.adminEmail())
                .subscriptionTier(SubscriptionTier.FREE_TRIAL)
                .subscriptionStatus(SubscriptionStatus.ACTIVE)
                .build();
        
        hospital = hospitalRepository.save(hospital);

        User admin = User.builder()
                .fullName(request.adminFullName())
                .email(request.adminEmail())
                .passwordHash(passwordEncoder.encode(request.adminPassword()))
                .role(Role.ADMIN)
                .hospital(hospital)
                .build();
        
        admin = userRepository.save(admin);
        return buildAuthResponse(admin);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (user.getHospital() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not associated with a hospital");
        }

        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        Long hospitalId = user.getHospital() != null ? user.getHospital().getId() : null;
        // Debug: log hospitalId to help diagnose missing tenant ID
        if (hospitalId == null) {
            System.out.println("[AUTH] WARNING: hospitalId is null for user " + user.getId() + " (" + user.getEmail() + "). " +
                    "user.getHospital() = " + user.getHospital());
        }
        String token = jwtService.generateToken(
                new UserPrincipal(user),
                Map.of("role", user.getRole().name(), "userId", user.getId(), "fullName", user.getFullName(), "hospitalId", hospitalId)
        );
        UserDto userDto = new UserDto(user.getId(), user.getEmail(), user.getRole(), hospitalId);
        return new AuthResponse(token, userDto);
    }
}

package com.healthtrack.service;

import com.healthtrack.dto.AuthDtos.*;
import com.healthtrack.entity.Role;
import com.healthtrack.entity.User;
import com.healthtrack.entity.Hospital;
import com.healthtrack.entity.SubscriptionTier;
import com.healthtrack.entity.SubscriptionStatus;
import com.healthtrack.entity.TokenBlocklist;
import com.healthtrack.repository.TokenBlocklistRepository;
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
import org.springframework.data.redis.core.StringRedisTemplate;
import java.util.concurrent.TimeUnit;

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
    private final StringRedisTemplate redisTemplate;
    private final TokenBlocklistRepository tokenBlocklistRepository;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        Role role = request.role() == null ? Role.PATIENT : request.role();

        Hospital hospital;
        if (request.invitationCode() != null && !request.invitationCode().isBlank()) {
            hospital = hospitalRepository.findByInvitationCode(request.invitationCode())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid invitation code"));
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invitation code is required");
        }

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

        String invitationCode = java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Hospital hospital = Hospital.builder()
                .name(request.hospitalName())
                .licenseNumber(request.licenseNumber())
                .contactEmail(request.adminEmail())
                .subscriptionTier(SubscriptionTier.FREE_TRIAL)
                .subscriptionStatus(SubscriptionStatus.ACTIVE)
                .invitationCode(invitationCode)
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

        user.setRefreshTokenVersion(user.getRefreshTokenVersion() + 1);
        userRepository.save(user);

        return buildAuthResponse(user);
    }

    public AuthResponse refresh(RefreshRequest request) {
        if (!jwtService.isRefreshTokenValid(request.refreshToken())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token");
        }

        String email = jwtService.extractUsername(request.refreshToken());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        Integer tokenVersion = jwtService.extractClaim(request.refreshToken(), claims -> claims.get("tokenVersion", Integer.class));
        if (tokenVersion == null || !tokenVersion.equals(user.getRefreshTokenVersion())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token version");
        }

        user.setRefreshTokenVersion(user.getRefreshTokenVersion() + 1);
        userRepository.save(user);

        UserPrincipal principal = new UserPrincipal(user);
        Long hospitalId = user.getHospital() != null ? user.getHospital().getId() : null;

        JwtService.TokenPair pair = jwtService.generateTokenPair(
                principal,
                Map.of("role", user.getRole().name(), "userId", user.getId(), "fullName", user.getFullName(), "hospitalId", hospitalId, "tokenVersion", user.getRefreshTokenVersion())
        );

        // Blocklist the old refresh token to prevent reuse
        try {
            long expirationTime = jwtService.getExpiration(request.refreshToken()).getTime();
            long ttl = expirationTime - System.currentTimeMillis();
            if (ttl > 0) {
                redisTemplate.opsForValue().set("blocklist:" + request.refreshToken(), "true", ttl, TimeUnit.MILLISECONDS);
                tokenBlocklistRepository.save(TokenBlocklist.builder()
                        .token(request.refreshToken())
                        .expiresAt(java.time.OffsetDateTime.ofInstant(java.time.Instant.ofEpochMilli(expirationTime), java.time.ZoneOffset.UTC))
                        .blockedAt(java.time.OffsetDateTime.now())
                        .build());
            }
        } catch (Exception ignored) {}

        return new AuthResponse(pair.accessToken(), pair.refreshToken(), null);
    }

    public void logout(String accessToken) {
        if (accessToken != null && !accessToken.isBlank()) {
            try {
                long expirationTime = jwtService.getExpiration(accessToken).getTime();
                long ttl = expirationTime - System.currentTimeMillis();
                if (ttl > 0) {
                    redisTemplate.opsForValue().set("blocklist:" + accessToken, "true", ttl, TimeUnit.MILLISECONDS);
                    if (!tokenBlocklistRepository.existsByToken(accessToken)) {
                        tokenBlocklistRepository.save(TokenBlocklist.builder()
                                .token(accessToken)
                                .expiresAt(java.time.OffsetDateTime.ofInstant(java.time.Instant.ofEpochMilli(expirationTime), java.time.ZoneOffset.UTC))
                                .blockedAt(java.time.OffsetDateTime.now())
                                .build());
                    }
                }
            } catch (Exception e) {
                // Token might be malformed or already expired, ignore
            }
        }
    }

    private AuthResponse buildAuthResponse(User user) {
        Long hospitalId = user.getHospital() != null ? user.getHospital().getId() : null;
        if (hospitalId == null) {
            System.out.println("[AUTH] WARNING: hospitalId is null for user " + user.getId() + " (" + user.getEmail() + "). " +
                    "user.getHospital() = " + user.getHospital());
        }
        UserPrincipal principal = new UserPrincipal(user);
        JwtService.TokenPair pair = jwtService.generateTokenPair(
                principal,
                Map.of("role", user.getRole().name(), "userId", user.getId(), "fullName", user.getFullName(), "hospitalId", hospitalId, "tokenVersion", user.getRefreshTokenVersion())
        );
        UserDto userDto = new UserDto(user.getId(), user.getEmail(), user.getRole(), hospitalId);
        return new AuthResponse(pair.accessToken(), pair.refreshToken(), userDto);
    }
}

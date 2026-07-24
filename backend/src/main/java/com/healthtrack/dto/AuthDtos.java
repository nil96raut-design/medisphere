package com.healthtrack.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.healthtrack.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class AuthDtos {

    public record RegisterRequest(
            @NotBlank String fullName,
            @Email @NotBlank String email,
            @NotBlank String password,
            Role role,
            Long primaryDoctorId,
            String allergies
    ) {}

    public record LoginRequest(
            @Email @NotBlank String email,
            @NotBlank String password
    ) {}

    public record HospitalSignupRequest(
            @NotBlank String hospitalName,
            @NotBlank String licenseNumber,
            @Email @NotBlank String adminEmail,
            @NotBlank String adminFullName,
            @NotBlank String adminPassword
    ) {}

    public record UserDto(
            Long id,
            String username,
            Role role,
            Long hospitalId
    ) {}

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record AuthResponse(
            String token,
            UserDto user
    ) {}
}

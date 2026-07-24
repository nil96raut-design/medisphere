package com.healthtrack.dto;

import com.healthtrack.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class UserDtos {
    public record UserSummary(Long id, String fullName, String email, Role role) {}

    public record CreateUserRequest(
            @NotBlank String fullName,
            @Email @NotBlank String email,
            @NotBlank String password,
            @NotBlank Role role
    ) {}
}

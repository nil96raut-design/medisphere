package com.healthtrack.service;

import com.healthtrack.dto.AuthDtos.AuthResponse;
import com.healthtrack.dto.AuthDtos.LoginRequest;
import com.healthtrack.dto.AuthDtos.RegisterRequest;
import com.healthtrack.entity.Role;
import com.healthtrack.entity.User;
import com.healthtrack.repository.UserRepository;
import com.healthtrack.support.PostgresTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpStatus.CONFLICT;

class AuthServiceTest extends PostgresTestBase {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * AuthService.register() always looks up hospitalRepository.findById(1L)
     * as the default tenant.  We insert that row directly here (rather than
     * via the Hospital entity) to guarantee a predictable PK despite any
     * IDENTITY generation complexities with H2's PostgreSQL mode and
     * the test-managed transaction.
     */
    @BeforeEach
    void setUp() {
        jdbcTemplate.update("""
            INSERT INTO hospital (id, name, license_number, contact_email,
                                  created_at, subscription_tier, subscription_status)
            VALUES (1, 'Default Hospital', 'DEFAULT-HOSP',
                    'default@hospital.com', NOW(), 'MONTHLY', 'ACTIVE')
            """);
    }

    @Test
    void registerThenLogin_succeeds() {
        RegisterRequest register = new RegisterRequest("Ada Lovelace", "ada@example.com", "s3cret-pw", Role.PATIENT, null, null);
        AuthResponse registered = authService.register(register);

        assertThat(registered.token()).isNotBlank();
        assertThat(registered.user().role()).isEqualTo(Role.PATIENT);

        AuthResponse loggedIn = authService.login(new LoginRequest("ada@example.com", "s3cret-pw"));
        assertThat(loggedIn.user().id()).isEqualTo(registered.user().id());
    }

    @Test
    void register_withDuplicateEmail_isRejected() {
        RegisterRequest first = new RegisterRequest("Grace Hopper", "grace@example.com", "s3cret-pw", Role.DOCTOR, null, null);
        authService.register(first);

        RegisterRequest duplicate = new RegisterRequest("Grace H.", "grace@example.com", "other-pw", Role.DOCTOR, null, null);

        assertThatThrownBy(() -> authService.register(duplicate))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(CONFLICT.value());
    }

    @Test
    void login_withWrongPassword_isRejected() {
        authService.register(new RegisterRequest("Alan Turing", "alan@example.com", "correct-pw", Role.PATIENT, null, null));

        assertThatThrownBy(() -> authService.login(new LoginRequest("alan@example.com", "wrong-pw")))
                .isInstanceOf(Exception.class); // AuthenticationManager throws before our own 401 mapping
    }

    @Test
    void login_withUnknownEmail_isRejected() {
        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@example.com", "whatever")))
                .isInstanceOf(Exception.class);
    }

    @Test
    void register_persistsAllergies() {
        RegisterRequest register = new RegisterRequest(
                "Nina Patient", "nina@example.com", "s3cret-pw", Role.PATIENT, null, "penicillin, latex");

        authService.register(register);

        User saved = userRepository.findByEmail("nina@example.com").orElseThrow();
        assertThat(saved.getAllergies()).isEqualTo("penicillin, latex");
    }
}

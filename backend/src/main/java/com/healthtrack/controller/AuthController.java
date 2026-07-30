package com.healthtrack.controller;

import com.healthtrack.dto.AuthDtos.*;
import com.healthtrack.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    
    @Value("${app.jwt.expiration-ms:86400000}") // 1 day
    private int jwtExpirationMs;
    
    @Value("${app.jwt.refresh-expiration-ms:604800000}") // 7 days
    private int jwtRefreshExpirationMs;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
        AuthResponse authRes = authService.register(request);
        setCookies(response, authRes);
        return ResponseEntity.ok(new AuthResponse(null, null, authRes.user()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthResponse authRes = authService.login(request);
        setCookies(response, authRes);
        return ResponseEntity.ok(new AuthResponse(null, null, authRes.user()));
    }

    @PostMapping("/hospital-signup")
    public ResponseEntity<AuthResponse> hospitalSignup(@Valid @RequestBody HospitalSignupRequest request, HttpServletResponse response) {
        AuthResponse authRes = authService.hospitalSignup(request);
        setCookies(response, authRes);
        return ResponseEntity.ok(new AuthResponse(null, null, authRes.user()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@CookieValue(name = "ht_refresh_token", required = false) String refreshToken, HttpServletResponse response) {
        if (refreshToken == null) {
            return ResponseEntity.status(401).build();
        }
        AuthResponse authRes = authService.refresh(new RefreshRequest(refreshToken));
        setCookies(response, authRes);
        return ResponseEntity.ok(new AuthResponse(null, null, null));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "ht_access_token", required = false) String accessToken,
            HttpServletResponse response) {
        authService.logout(accessToken);
        clearCookies(response);
        return ResponseEntity.ok().build();
    }

    private void setCookies(HttpServletResponse response, AuthResponse authRes) {
        ResponseCookie accessCookie = ResponseCookie.from("ht_access_token", authRes.token())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(jwtExpirationMs / 1000)
                .sameSite("None")
                .build();
                
        ResponseCookie refreshCookie = ResponseCookie.from("ht_refresh_token", authRes.refreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(jwtRefreshExpirationMs / 1000)
                .sameSite("None")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }

    private void clearCookies(HttpServletResponse response) {
        ResponseCookie accessCookie = ResponseCookie.from("ht_access_token", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("None")
                .build();
                
        ResponseCookie refreshCookie = ResponseCookie.from("ht_refresh_token", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("None")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }
}

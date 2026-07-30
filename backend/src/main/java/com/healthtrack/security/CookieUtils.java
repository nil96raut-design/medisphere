package com.healthtrack.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class CookieUtils {

    public static final String JWT_COOKIE_NAME = "HT_JWT";

    public ResponseCookie createJwtCookie(String token, long maxAgeSeconds) {
        return ResponseCookie.from(JWT_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(true) // Enforce HTTPS
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .build();
    }

    public ResponseCookie createCleanJwtCookie() {
        return ResponseCookie.from(JWT_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build();
    }

    public String extractJwtFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (JWT_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}

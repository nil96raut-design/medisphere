package com.healthtrack.controller;

import com.healthtrack.entity.UserSession;
import com.healthtrack.security.UserPrincipal;
import com.healthtrack.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @GetMapping("/my-sessions")
    public ResponseEntity<List<UserSession>> getMySessions(@AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(sessionService.getUserSessions(currentUser.getUser().getId()));
    }

    @DeleteMapping("/{sessionId}/revoke")
    public ResponseEntity<Void> revokeSession(@PathVariable Long sessionId) {
        sessionService.revokeSession(sessionId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/users/{userId}/force-logout")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> forceLogoutUser(@PathVariable Long userId) {
        sessionService.forceLogoutUser(userId);
        return ResponseEntity.ok().build();
    }
}

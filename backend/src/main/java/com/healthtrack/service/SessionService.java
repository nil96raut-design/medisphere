package com.healthtrack.service;

import com.healthtrack.entity.UserSession;
import com.healthtrack.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SessionService {

    private static final Logger log = LoggerFactory.getLogger(SessionService.class);

    private final UserSessionRepository sessionRepository;

    @Transactional
    public UserSession createSession(String token, Long userId, Long hospitalId, String ipAddress, String userAgent) {
        UserSession session = UserSession.builder()
                .sessionToken(token)
                .userId(userId)
                .hospitalId(hospitalId)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .isRevoked(false)
                .build();
        return sessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    public boolean isSessionActive(String token) {
        return sessionRepository.findBySessionToken(token)
                .map(s -> !Boolean.TRUE.equals(s.getIsRevoked()))
                .orElse(true); // Default allow if legacy session
    }

    @Transactional
    public void touchSession(String token) {
        sessionRepository.findBySessionToken(token).ifPresent(s -> {
            s.setLastActiveAt(OffsetDateTime.now());
            sessionRepository.save(s);
        });
    }

    @Transactional(readOnly = true)
    public List<UserSession> getUserSessions(Long userId) {
        return sessionRepository.findByUserIdAndIsRevokedFalse(userId);
    }

    @Transactional
    public void revokeSession(Long sessionId) {
        UserSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
        session.setIsRevoked(true);
        sessionRepository.save(session);
        log.info("Session ID {} revoked", sessionId);
    }

    @Transactional
    public void forceLogoutUser(Long userId) {
        List<UserSession> activeSessions = sessionRepository.findByUserIdAndIsRevokedFalse(userId);
        for (UserSession s : activeSessions) {
            s.setIsRevoked(true);
        }
        sessionRepository.saveAll(activeSessions);
        log.info("Force logged out user ID: {} across {} active sessions", userId, activeSessions.size());
    }
}

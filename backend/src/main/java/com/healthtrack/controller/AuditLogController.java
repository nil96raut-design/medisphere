package com.healthtrack.controller;

import com.healthtrack.dto.AuditLogDtos;
import com.healthtrack.entity.AuditLog;
import com.healthtrack.repository.AuditLogRepository;
import com.healthtrack.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/admin/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping
    public ResponseEntity<Page<AuditLogDtos.AuditLogEntry>> getAuditLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        Page<AuditLog> logs = auditLogRepository.search(
                currentUser.getHospitalId(), action, userId, from, to,
                PageRequest.of(page, Math.min(size, 100)));

        return ResponseEntity.ok(logs.map(log -> new AuditLogDtos.AuditLogEntry(
                log.getId(), log.getUserId(), log.getAction(), log.getEntity(),
                log.getEntityId(), log.getDetails(), log.getTimestamp())));
    }
}

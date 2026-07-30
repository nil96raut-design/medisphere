package com.healthtrack.controller;

import com.healthtrack.service.RecoveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/recovery")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class RecoveryController {

    private final RecoveryService recoveryService;

    @GetMapping("/dead-letters")
    public ResponseEntity<Map<String, Object>> getDeadLetterEvents() {
        return ResponseEntity.ok(recoveryService.getDeadLetterEvents());
    }

    @PostMapping("/dead-letters/{category}/{id}/replay")
    public ResponseEntity<Void> replayDeadLetter(
            @PathVariable String category,
            @PathVariable Long id) {
        recoveryService.replayDeadLetter(category, id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/dead-letters/replay-all")
    public ResponseEntity<Map<String, Integer>> replayAllDeadLetters() {
        return ResponseEntity.ok(recoveryService.replayAllDeadLetters());
    }
}

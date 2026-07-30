package com.healthtrack.controller;

import com.healthtrack.aspect.ChaosMonkeyAspect;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/chaos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ChaosController {

    @GetMapping("/status")
    public ResponseEntity<Map<String, Boolean>> getChaosStatus() {
        Map<String, Boolean> status = new HashMap<>();
        status.put("dbOutageActive", ChaosMonkeyAspect.isDbOutageActive());
        status.put("redisOutageActive", ChaosMonkeyAspect.isRedisOutageActive());
        status.put("kafkaDelayActive", ChaosMonkeyAspect.isKafkaDelayActive());
        return ResponseEntity.ok(status);
    }

    @PostMapping("/db-outage")
    public ResponseEntity<Map<String, String>> toggleDbOutage(@RequestParam boolean active) {
        ChaosMonkeyAspect.setDbOutageActive(active);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Simulated DB Outage state set to: " + active);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/redis-outage")
    public ResponseEntity<Map<String, String>> toggleRedisOutage(@RequestParam boolean active) {
        ChaosMonkeyAspect.setRedisOutageActive(active);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Simulated Redis Outage state set to: " + active);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/kafka-delay")
    public ResponseEntity<Map<String, String>> toggleKafkaDelay(@RequestParam boolean active) {
        ChaosMonkeyAspect.setKafkaDelayActive(active);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Simulated Kafka Delay state set to: " + active);
        return ResponseEntity.ok(response);
    }
}

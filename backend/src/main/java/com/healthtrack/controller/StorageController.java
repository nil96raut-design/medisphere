package com.healthtrack.controller;

import com.healthtrack.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/storage")
@RequiredArgsConstructor
public class StorageController {

    private final StorageService storageService;

    @GetMapping("/signed-url")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE', 'LAB_TECH', 'ADMIN')")
    public ResponseEntity<Map<String, String>> getSignedUrl(
            @RequestParam String bucket,
            @RequestParam String filePath,
            @RequestParam(defaultValue = "15") int expiresMinutes) {
        String url = storageService.generatePreSignedUrl(bucket, filePath, expiresMinutes);
        return ResponseEntity.ok(Map.of("signedUrl", url, "expiresInMinutes", String.valueOf(expiresMinutes)));
    }
}

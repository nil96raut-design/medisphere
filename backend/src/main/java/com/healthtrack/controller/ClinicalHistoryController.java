package com.healthtrack.controller;

import com.healthtrack.entity.ClinicalRecordHistory;
import com.healthtrack.service.ClinicalHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/clinical-records")
@RequiredArgsConstructor
public class ClinicalHistoryController {

    private final ClinicalHistoryService clinicalHistoryService;

    @GetMapping("/{recordType}/{recordId}/history")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE', 'ADMIN')")
    public ResponseEntity<List<ClinicalRecordHistory>> getHistory(
            @PathVariable String recordType,
            @PathVariable Long recordId) {
        return ResponseEntity.ok(clinicalHistoryService.getHistory(recordType, recordId));
    }
}

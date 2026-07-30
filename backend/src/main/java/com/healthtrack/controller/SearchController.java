package com.healthtrack.controller;

import com.healthtrack.security.UserPrincipal;
import com.healthtrack.service.GlobalSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final GlobalSearchService globalSearchService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> search(
            @RequestParam String q,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(globalSearchService.globalSearch(q, currentUser.getHospitalId()));
    }
}

package com.healthtrack.controller;

import com.healthtrack.security.UserPrincipal;
import com.healthtrack.service.SseNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class SseController {

    private final SseNotificationService sseNotificationService;

    @GetMapping("/stream")
    public SseEmitter streamEvents(@AuthenticationPrincipal UserPrincipal currentUser) {
        return sseNotificationService.subscribe(currentUser.getHospitalId());
    }
}

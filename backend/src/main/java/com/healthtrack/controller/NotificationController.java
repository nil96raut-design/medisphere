package com.healthtrack.controller;

import com.healthtrack.dto.NotificationDtos.NotificationResponse;
import com.healthtrack.entity.Notification;
import com.healthtrack.repository.NotificationRepository;
import com.healthtrack.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<NotificationResponse>> getMyNotifications(
            Pageable pageable,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Long userId = currentUser.getUser().getId();
        Page<Notification> notifications = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return ResponseEntity.ok(notifications.map(this::mapToResponse));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Long> getUnreadCount(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(
                notificationRepository.countByUserIdAndStatus(currentUser.getUser().getId(), "SENT"));
    }

    @PutMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setStatus("READ");
            notificationRepository.save(n);
        });
        return ResponseEntity.noContent().build();
    }

    private NotificationResponse mapToResponse(Notification n) {
        return new NotificationResponse(
                n.getId(), n.getType(), n.getTitle(), n.getMessage(),
                n.getStatus(), n.getReferenceType(), n.getReferenceId(), n.getSentAt());
    }
}

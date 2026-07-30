package com.healthtrack.dto;

import java.time.OffsetDateTime;

public class NotificationDtos {

    public record NotificationResponse(
            Long id,
            String type,
            String title,
            String message,
            String status,
            String referenceType,
            Long referenceId,
            OffsetDateTime sentAt
    ) {}
}

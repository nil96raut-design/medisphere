package com.healthtrack.dto;

import java.time.OffsetDateTime;

public class AuditLogDtos {

    public record AuditLogEntry(
            Long id,
            Long userId,
            String action,
            String entity,
            Long entityId,
            String details,
            OffsetDateTime timestamp
    ) {}
}

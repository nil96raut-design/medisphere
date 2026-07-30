package com.healthtrack.dto;

import java.time.OffsetDateTime;
import java.util.Map;

public record TimelineEventDto(
        Long id,
        String eventType,
        String title,
        String description,
        OffsetDateTime timestamp,
        Map<String, Object> metadata
) {}

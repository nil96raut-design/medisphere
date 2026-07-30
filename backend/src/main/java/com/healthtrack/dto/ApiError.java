package com.healthtrack.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.http.HttpStatusCode;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record ApiError(
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        String correlationId
) {
    public ApiError(HttpStatusCode status, String message, String path, String correlationId) {
        this(OffsetDateTime.now(ZoneOffset.UTC), status.value(), status.toString(), message, path, correlationId);
    }
}

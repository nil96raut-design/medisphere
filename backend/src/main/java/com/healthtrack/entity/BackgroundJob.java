package com.healthtrack.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "background_job")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BackgroundJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String jobType;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Builder.Default
    @Column(nullable = false)
    private String status = "PENDING";

    @Builder.Default
    @Column(nullable = false)
    private Integer priority = 5;

    @Builder.Default
    @Column(nullable = false)
    private Integer retryCount = 0;

    @Builder.Default
    @Column(nullable = false)
    private Integer maxRetries = 3;

    @Column(columnDefinition = "TEXT")
    private String lastError;

    private OffsetDateTime nextAttemptAt;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    private OffsetDateTime completedAt;

    private OffsetDateTime lockedUntil;
}

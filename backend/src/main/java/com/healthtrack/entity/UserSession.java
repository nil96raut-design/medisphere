package com.healthtrack.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "user_session")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String sessionToken;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long hospitalId;

    private String ipAddress;

    @Column(columnDefinition = "text")
    private String userAgent;

    @Column(nullable = false)
    private Boolean isRevoked;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime lastActiveAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (lastActiveAt == null) {
            lastActiveAt = OffsetDateTime.now();
        }
        if (isRevoked == null) {
            isRevoked = false;
        }
    }
}

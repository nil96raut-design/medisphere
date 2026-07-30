package com.healthtrack.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;

@Entity
@Table(name = "write_buffer")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WriteBufferEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String operationType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, Object> payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QueuePriority priority;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private Integer retryCount;

    @Column(columnDefinition = "text")
    private String errorMessage;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    private OffsetDateTime processedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (status == null) {
            status = "PENDING";
        }
        if (retryCount == null) {
            retryCount = 0;
        }
    }
}

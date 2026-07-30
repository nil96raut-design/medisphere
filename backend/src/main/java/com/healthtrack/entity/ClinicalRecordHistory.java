package com.healthtrack.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;

@Entity
@Table(name = "clinical_record_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClinicalRecordHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String recordType;

    @Column(nullable = false)
    private Long recordId;

    @Column(nullable = false)
    private Integer versionNumber;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, Object> snapshotPayload;

    @Column(nullable = false, length = 100)
    private String modifiedBy;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime modifiedAt;

    @Column(columnDefinition = "text")
    private String changeReason;

    @PrePersist
    protected void onCreate() {
        if (modifiedAt == null) {
            modifiedAt = OffsetDateTime.now();
        }
    }
}

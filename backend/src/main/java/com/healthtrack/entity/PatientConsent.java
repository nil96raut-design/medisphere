package com.healthtrack.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "patient_consent")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientConsent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long patientId;

    @Column(nullable = false, length = 50)
    private String grantedRole;

    @Column(nullable = false, length = 50)
    private String consentType;

    @Column(nullable = false)
    private Boolean isGranted;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime grantedAt;

    private OffsetDateTime expiresAt;

    @Column(columnDefinition = "text")
    private String notes;

    @PrePersist
    protected void onCreate() {
        if (grantedAt == null) {
            grantedAt = OffsetDateTime.now();
        }
        if (isGranted == null) {
            isGranted = true;
        }
    }
}

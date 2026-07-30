package com.healthtrack.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "shift_handover")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "hospital_id = :hospitalId")
public class ShiftHandover {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_nurse_id", nullable = false)
    private User fromNurse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_nurse_id", nullable = false)
    private User toNurse;

    @Column(length = 100)
    private String wardName;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "patient_summary", columnDefinition = "TEXT")
    private String patientSummary;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime createdAt = OffsetDateTime.now();
}

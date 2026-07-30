package com.healthtrack.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lab_test_order")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "hospital_id = :hospitalId")
public class LabTestOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medical_record_id")
    private MedicalRecord medicalRecord;

    @Column(nullable = false)
    private String testName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by", nullable = false)
    private User requestedBy;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private LabOrderStatus status = LabOrderStatus.ORDERED;

    @Column(columnDefinition = "TEXT")
    private String resultValues;

    @Column(columnDefinition = "TEXT")
    private String technicianNotes;

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal price = BigDecimal.ZERO;

    private LocalDateTime completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    private OffsetDateTime approvedAt;

    private LocalDateTime sampleCollectedAt;

    private OffsetDateTime resultEnteredAt;

    private LocalDateTime processingStartedAt;

    private LocalDateTime processingCompletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "result_entered_by")
    private User resultEnteredBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "retest_of")
    private LabTestOrder retestOf;

    @Column(columnDefinition = "TEXT")
    private String correctionReason;

    private Integer turnaroundMinutes;

    @Builder.Default
    @Column(nullable = false)
    private Boolean criticalFlag = false;

    private String sampleBarcode;

    private String sampleStorageLocation;

    @Builder.Default
    @OneToMany(mappedBy = "labOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SampleTracking> sampleTrackings = new ArrayList<>();

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}

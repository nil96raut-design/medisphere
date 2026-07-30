package com.healthtrack.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.time.LocalDateTime;

@Entity
@Table(name = "sample_tracking")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "hospital_id = :hospitalId")
public class SampleTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lab_order_id", nullable = false)
    private LabTestOrder labOrder;

    @Column(nullable = false)
    private String sampleType;

    private String containerType;

    private String barcode;

    private String collectionVolume;

    private String collectionMethod;

    private String storageLocation;

    private String storageCondition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collected_by", nullable = false)
    private User collectedBy;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime collectedAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private SampleStatus status = SampleStatus.COLLECTED;

    private LocalDateTime disposedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disposed_by")
    private User disposedBy;

    @Builder.Default
    @Column(nullable = false)
    private Integer retentionDays = 30;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}

package com.healthtrack.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "vital_record")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "hospital_id = :hospitalId")
public class VitalRecord {

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
    @JoinColumn(name = "nurse_id", nullable = false)
    private User nurse;

    @Column(length = 20)
    private String bloodPressure;

    private Integer heartRate;

    @Column(precision = 4, scale = 1)
    private BigDecimal temperature;

    private Integer spo2;

    @Column(precision = 5, scale = 2)
    private BigDecimal sugarLevel;

    @Builder.Default
    @Column(nullable = false)
    private Boolean alertFlag = false;

    @Column(length = 255)
    private String alertReason;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime recordedAt = OffsetDateTime.now();
}

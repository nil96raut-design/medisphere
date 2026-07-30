package com.healthtrack.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.time.LocalDateTime;

@Entity
@Table(name = "sla_breach")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "hospital_id = :hospitalId")
public class SlaBreach {

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
    private Integer expectedTatMinutes;

    @Column(nullable = false)
    private Integer actualTatMinutes;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime breachedAt = LocalDateTime.now();

    @Builder.Default
    @Column(nullable = false)
    private Boolean notified = false;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}

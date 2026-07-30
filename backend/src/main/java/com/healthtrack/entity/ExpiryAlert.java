package com.healthtrack.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "expiry_alert")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "hospital_id = :hospitalId")
public class ExpiryAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicine_stock_id", nullable = false)
    private MedicineStock medicineStock;

    @Column(name = "medicine_name", nullable = false)
    private String medicineName;

    @Column(name = "batch_number", nullable = false)
    private String batchNumber;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 20)
    private ExpiryAlertType alertType;

    @Builder.Default
    @Column(name = "is_resolved", nullable = false)
    private Boolean isResolved = false;

    @Builder.Default
    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "resolved_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime resolvedAt;
}

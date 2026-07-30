package com.healthtrack.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "dispensation_record")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "hospital_id = :hospitalId")
public class DispensationRecord {

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
    @JoinColumn(name = "prescription_item_id")
    private PrescriptionItem prescriptionItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicine_stock_id")
    private MedicineStock medicineStock;

    @Column(nullable = false)
    private String medicineName;

    @Column(nullable = false)
    private Integer quantityDispensed;

    @Column(name = "remaining_quantity")
    @Builder.Default
    private Integer remainingQuantity = 0;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private BillingStatus billingStatus = BillingStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "dispensation_status", length = 20)
    @Builder.Default
    private DispensationStatus dispensationStatus = DispensationStatus.COMPLETE;

    @Column(name = "batch_id")
    private Long batchId;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime dispensedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispensed_by", nullable = false)
    private User dispensedBy;
}

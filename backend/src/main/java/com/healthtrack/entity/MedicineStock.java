package com.healthtrack.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "medicine_stock")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "hospital_id = :hospitalId")
public class MedicineStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    @Column(nullable = false)
    private String medicineName;

    @Column(nullable = false)
    private String batchNumber;

    @Column(nullable = false)
    private LocalDate expiryDate;

    @Builder.Default
    @Column(nullable = false)
    private Integer availableQuantity = 0;

    @Builder.Default
    @Column(name = "quantity_reserved", nullable = false)
    private Integer quantityReserved = 0;

    @Builder.Default
    @Column(nullable = false)
    private Integer reorderLevel = 10;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(name = "purchase_price", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal purchasePrice = BigDecimal.ZERO;

    @Column(name = "selling_price", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal sellingPrice = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Builder.Default
    @Column(name = "created_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public Integer getEffectiveQuantity() {
        return availableQuantity - quantityReserved;
    }

    public boolean isExpired() {
        return expiryDate != null && expiryDate.isBefore(LocalDate.now());
    }

    public boolean isLowStock() {
        return getEffectiveQuantity() <= reorderLevel;
    }
}

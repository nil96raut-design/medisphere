package com.healthtrack.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "purchase_order")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "hospital_id = :hospitalId")
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Column(name = "medicine_name")
    private String medicineName;

    @Column(name = "quantity", nullable = false)
    private Integer quantityOrdered;

    @Column(name = "quantity_received")
    @Builder.Default
    private Integer quantityReceived = 0;

    @Column(name = "unit_price", precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_price", precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 50)
    private PurchaseOrderStatus status = PurchaseOrderStatus.CREATED;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Builder.Default
    @Column(name = "ordered_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime orderedAt = OffsetDateTime.now();

    @Column(name = "received_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime receivedAt;
}

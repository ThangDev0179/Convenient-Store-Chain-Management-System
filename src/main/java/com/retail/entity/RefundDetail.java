package com.retail.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Maps to dbo.RefundDetail — DO NOT rename any column.
 */
@Entity
@Table(name = "RefundDetail")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RefundDetailId")
    private Long refundDetailId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "RefundId", nullable = false)
    private Refund refund;

    /** FK → Product. Not mapped as @ManyToOne (cross-module). */
    @Column(name = "ProductId", nullable = false)
    private Long productId;

    /**
     * Quantity to return. > 0 (Rule #11).
     * Rule #5: cumulative quantity refunded for this product+invoice must not exceed original sold quantity.
     */
    @Column(name = "Quantity", precision = 18, scale = 3, nullable = false)
    private BigDecimal quantity;

    /**
     * Resalable or Damaged.
     * Determines inventory handling in 3.2.3.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "ConditionType", length = 20, nullable = false)
    private ConditionType conditionType;

    /**
     * = UnitPrice from the original InvoiceDetail (not client-entered).
     * Rule: no arbitrary refund amounts — price locked to original sale price.
     */
    @Column(name = "UnitRefundAmount", precision = 18, scale = 2, nullable = false)
    private BigDecimal unitRefundAmount;
}

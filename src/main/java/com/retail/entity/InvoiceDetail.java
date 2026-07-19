package com.retail.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Formula;

import java.math.BigDecimal;

/**
 * Maps to dbo.InvoiceDetail — DO NOT rename any column.
 *
 * IMPORTANT — LineTotal computed column:
 *   The DB defines LineTotal AS (Quantity * UnitPrice) PERSISTED.
 *   We map it with @Formula (Hibernate read-only formula) so Hibernate
 *   reads it from DB but never tries to INSERT or UPDATE it.
 *   The @Column insertable/updatable=false approach is an alternative but
 *   @Formula is cleaner for server-computed columns in Hibernate.
 */
@Entity
@Table(name = "InvoiceDetail")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "InvoiceDetailId")
    private Long invoiceDetailId;

    /**
     * ManyToOne back-reference to Invoice.
     * FetchType.LAZY: don't load parent Invoice when loading a standalone detail.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "InvoiceId", nullable = false)
    private Invoice invoice;

    /**
     * FK → Product. Not mapped as @ManyToOne to avoid cross-module dependency;
     * Product data is fetched via ProductStub repository in service layer when needed.
     */
    @Column(name = "ProductId", nullable = false)
    private Long productId;

    /**
     * Quantity > 0, validated by @DecimalMin("0.001") in DTO layer.
     * DECIMAL(18,3) in DB.
     */
    @Column(name = "Quantity", precision = 18, scale = 3, nullable = false)
    private BigDecimal quantity;

    /**
     * Final POS price at time of sale (after promotion, if any).
     * Captured at sale time — NOT affected by future price changes.
     */
    @Column(name = "UnitPrice", precision = 18, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    /**
     * FK → Promotion (nullable). Set when a promotion is applied at sale time.
     * Only 1 promotion per line item. Selection strategy (if multiple active promotions):
     * choose the one with the most recent StartDateTime (closest to now).
     * See InvoiceServiceImpl.findBestActivePromotion() for implementation.
     */
    @Column(name = "PromotionId")
    private Long promotionId;

    /**
     * READ-ONLY: computed column from DB (Quantity * UnitPrice).
     * Hibernate will load this value from DB but will NOT include it in INSERT/UPDATE.
     * Use invoice.recalculateTotalAmount() in service to keep Invoice.TotalAmount in sync.
     */
    @Formula("(Quantity * UnitPrice)")
    private BigDecimal lineTotal;
}

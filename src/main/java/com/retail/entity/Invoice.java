package com.retail.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Maps to dbo.Invoice — DO NOT rename any column.
 */
@Entity
@Table(name = "Invoice")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "InvoiceId")
    private Long invoiceId;

    /**
     * Format: INV-[BranchCode]-YYYYMMDD-[6 số tự tăng trong ngày].
     * Generated in service layer, stored here as immutable after creation.
     */
    @Column(name = "InvoiceCode", length = 40, nullable = false, unique = true, updatable = false)
    private String invoiceCode;

    @Column(name = "BranchId", nullable = false)
    private Integer branchId;

    /**
     * FK → Employee. Cashier logged in at the time of sale.
     * Not mapped as @ManyToOne to avoid cross-module entity dependency;
     * join to EmployeeStub happens in service layer when needed.
     */
    @Column(name = "CashierId", nullable = false)
    private Long cashierId;

    /**
     * Mapped as EnumType.STRING to match NVARCHAR column and CHECK constraint.
     * State transitions enforced by InvoiceStatus.canTransitionTo().
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "Status", length = 20, nullable = false)
    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.Draft;

    /**
     * NULL until Invoice is Paid. Then must be one of: Cash | QR | Bank | Card.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "PaymentMethod", length = 20)
    private PaymentMethod paymentMethod;

    /**
     * Maintained by service layer: sum of (Quantity * UnitPrice) for all InvoiceDetails.
     * LineTotal is a computed column in DB and is NOT written here.
     */
    @Column(name = "TotalAmount", precision = 18, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "CreatedAt", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "PaidAt")
    private LocalDateTime paidAt;

    @Column(name = "CanceledAt")
    private LocalDateTime canceledAt;

    /**
     * OneToMany: CascadeType.ALL so InvoiceDetails are persisted/removed with Invoice.
     * orphanRemoval = true: removing a detail from the list deletes it from DB.
     * FetchType.LAZY: details are loaded on demand.
     */
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<InvoiceDetail> details = new ArrayList<>();

    // ─── Domain helpers ──────────────────────────────────────────────────────────

    public void recalculateTotalAmount() {
        this.totalAmount = details.stream()
                .map(d -> d.getUnitPrice().multiply(d.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void addDetail(InvoiceDetail detail) {
        detail.setInvoice(this);
        this.details.add(detail);
        recalculateTotalAmount();
    }

    public void removeDetail(InvoiceDetail detail) {
        this.details.remove(detail);
        detail.setInvoice(null);
        recalculateTotalAmount();
    }
}

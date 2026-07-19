package com.retail.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "SupplierInvoice")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SupplierInvoiceId")
    private Long supplierInvoiceId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "GrnId", unique = true, nullable = false)
    private GoodsReceiptNote grn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SupplierId", nullable = false)
    private Supplier supplier;

    @Column(name = "Amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "AmountPaid", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Column(name = "Status", nullable = false, length = 20)
    @Builder.Default
    private String status = "Draft"; // Draft, Approved, Unpaid, Partially_Paid, Paid

    @Column(name = "IssuedAt", nullable = false)
    private LocalDateTime issuedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ApprovedBy")
    private Employee approvedBy;

    @Column(name = "ApprovedAt")
    private LocalDateTime approvedAt;

    @PrePersist
    protected void onCreate() {
        if (issuedAt == null) {
            issuedAt = LocalDateTime.now();
        }
        if (amountPaid == null) {
            amountPaid = BigDecimal.ZERO;
        }
        if (status == null) {
            status = "Draft";
        }
    }
}

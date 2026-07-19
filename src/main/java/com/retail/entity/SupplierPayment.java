package com.retail.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "SupplierPayment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SupplierPaymentId")
    private Long supplierPaymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SupplierInvoiceId", nullable = false)
    private SupplierInvoice supplierInvoice;

    @Column(name = "AmountPaid", nullable = false, precision = 18, scale = 2)
    private BigDecimal amountPaid;

    @Column(name = "PaymentMethod", nullable = false, length = 20)
    private String paymentMethod; // Cash, Bank, QR, Card

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PaidBy", nullable = false)
    private Employee paidBy;

    @Column(name = "PaidAt", nullable = false)
    private LocalDateTime paidAt;

    @PrePersist
    protected void onCreate() {
        if (paidAt == null) {
            paidAt = LocalDateTime.now();
        }
    }
}

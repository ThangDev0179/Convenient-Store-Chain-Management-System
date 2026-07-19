package com.retail.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @Column(name = "InvoiceCode", unique = true, nullable = false, length = 40)
    private String invoiceCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BranchId", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CashierId", nullable = false)
    private Employee cashier;

    @Column(name = "Status", nullable = false, length = 20)
    private String status; // Draft | Held | Paid | Canceled

    @Column(name = "PaymentMethod", length = 20)
    private String paymentMethod; // Cash | QR | Bank | Card

    @Column(name = "TotalAmount", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "PaidAt")
    private LocalDateTime paidAt;

    @Column(name = "CanceledAt")
    private LocalDateTime canceledAt;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<InvoiceDetail> details = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = "Draft";
        if (totalAmount == null) totalAmount = BigDecimal.ZERO;
    }

    public void addDetail(InvoiceDetail detail) {
        details.add(detail);
        detail.setInvoice(this);
    }
}

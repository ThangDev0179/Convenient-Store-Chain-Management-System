package com.retail.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "InventoryTransactionHistory")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryTransactionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TransactionId")
    private Long transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BranchId", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProductId", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "TransactionType", nullable = false, length = 30)
    private InventoryTransactionType transactionType;

    @Column(name = "ReferenceTable", nullable = false, length = 50)
    private String referenceTable;

    @Column(name = "ReferenceId", nullable = false)
    private Long referenceId;

    @Column(name = "QuantityChange", nullable = false, precision = 18, scale = 3)
    private BigDecimal quantityChange;

    @Column(name = "Reason", length = 500)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CreatedBy")
    private Employee createdBy;

    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
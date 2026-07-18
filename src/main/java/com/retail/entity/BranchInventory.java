package com.retail.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "BranchInventory")
@IdClass(BranchInventoryId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchInventory {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BranchId", nullable = false)
    private Branch branch;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProductId", nullable = false)
    private Product product;

    @Column(name = "QtyOnHand", nullable = false, precision = 18, scale = 3)
    private BigDecimal qtyOnHand;

    @Column(name = "QtyAvailable", nullable = false, precision = 18, scale = 3)
    private BigDecimal qtyAvailable;

    @Column(name = "QtyInTransit", nullable = false, precision = 18, scale = 3)
    private BigDecimal qtyInTransit;

    @Column(name = "UpdatedAt", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (qtyOnHand == null) qtyOnHand = BigDecimal.ZERO;
        if (qtyAvailable == null) qtyAvailable = BigDecimal.ZERO;
        if (qtyInTransit == null) qtyInTransit = BigDecimal.ZERO;
    }
}
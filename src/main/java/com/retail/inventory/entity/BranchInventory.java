package com.retail.inventory.entity;

import com.retail.branch.Branch;
import com.retail.product.Product;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "BranchInventory")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchInventory {

    @EmbeddedId
    private BranchInventoryId id;

    // @MapsId: JPA manages the FK column via the embedded ID,
    // do NOT use insertable=false/updatable=false alongside @MapsId
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("branchId")
    @JoinColumn(name = "BranchId")
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("productId")
    @JoinColumn(name = "ProductId")
    private Product product;

    @Column(name = "QtyOnHand", nullable = false, precision = 18, scale = 3)
    @Builder.Default
    private BigDecimal qtyOnHand = BigDecimal.ZERO;

    @Column(name = "QtyAvailable", nullable = false, precision = 18, scale = 3)
    @Builder.Default
    private BigDecimal qtyAvailable = BigDecimal.ZERO;

    @Column(name = "QtyInTransit", nullable = false, precision = 18, scale = 3)
    @Builder.Default
    private BigDecimal qtyInTransit = BigDecimal.ZERO;

    @Column(name = "UpdatedAt", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        if (this.updatedAt == null) this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

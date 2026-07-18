package com.retail.procurement;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "GoodsReceiptNoteDetail")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsReceiptNoteDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "GrnDetailId")
    private Long grnDetailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "GrnId", nullable = false)
    private GoodsReceiptNote goodsReceiptNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProductId", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UomId", nullable = false)
    private ProductUOM uom;

    @Column(name = "QuantityOrdered", nullable = false, precision = 18, scale = 3)
    private BigDecimal quantityOrdered;

    @Column(name = "QuantityReceived", nullable = false, precision = 18, scale = 3)
    private BigDecimal quantityReceived;

    @Column(name = "UnitCost", nullable = false, precision = 18, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "TotalCost", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalCost;

    @PrePersist
    @PreUpdate
    public void calculateTotalCost() {
        if (quantityReceived != null && unitCost != null) {
            this.totalCost = quantityReceived.multiply(unitCost);
        } else {
            this.totalCost = BigDecimal.ZERO;
        }
    }
}

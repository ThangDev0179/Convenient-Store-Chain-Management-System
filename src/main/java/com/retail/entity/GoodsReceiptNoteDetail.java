package com.retail.entity;

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

    @Column(name = "QuantityReceived", nullable = false, precision = 18, scale = 3)
    private BigDecimal quantityReceived;

    @Column(name = "QuantityConvertedBase", nullable = false, precision = 18, scale = 3)
    private BigDecimal quantityConvertedBase;

    @PrePersist
    @PreUpdate
    public void calculateConvertedQuantity() {
        if (quantityReceived != null && uom != null) {
            this.quantityConvertedBase = quantityReceived.multiply(BigDecimal.valueOf(uom.getConversionRate()));
        }
    }
}
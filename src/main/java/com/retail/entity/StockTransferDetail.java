package com.retail.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "StockTransferDetail")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockTransferDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TransferDetailId")
    private Long transferDetailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StockTransferId", nullable = false)
    @ToString.Exclude
    private StockTransfer stockTransfer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProductId", nullable = false)
    @ToString.Exclude
    private Product product;

    @Column(name = "QuantitySent", nullable = false, precision = 18, scale = 3)
    private BigDecimal quantitySent;

    @Column(name = "QuantityReceived", precision = 18, scale = 3)
    private BigDecimal quantityReceived;

    /**
     * VarianceQty = QuantityReceived - QuantitySent.
     * Computed in DB (PERSISTED), mapped as read-only here.
     */
    @Column(name = "VarianceQty", insertable = false, updatable = false, precision = 19, scale = 3)
    private BigDecimal varianceQty;
}
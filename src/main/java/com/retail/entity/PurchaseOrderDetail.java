package com.retail.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "PurchaseOrderDetail")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PoDetailId")
    private Long poDetailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PurchaseOrderId", nullable = false)
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ProductId", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "UomId", nullable = false)
    private ProductUOM uom;

    @Column(name = "QuantityOrdered", nullable = false, precision = 18, scale = 3)
    private BigDecimal quantityOrdered;

    @Column(name = "UnitCost", precision = 18, scale = 2)
    private BigDecimal unitCost;
}
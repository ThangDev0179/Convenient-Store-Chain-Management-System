package com.retail.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "InventoryCountDetail")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryCountDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CountDetailId")
    private Long countDetailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "InventoryCountId", nullable = false)
    @ToString.Exclude
    private InventoryCount inventoryCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProductId", nullable = false)
    private Product product;

    @Column(name = "SystemQty", nullable = false, precision = 18, scale = 3)
    private BigDecimal systemQty;

    @Column(name = "ActualQty", nullable = false, precision = 18, scale = 3)
    private BigDecimal actualQty;

    @Column(name = "DeltaQty", insertable = false, updatable = false, precision = 19, scale = 3)
    private BigDecimal deltaQty; // Computed column in DB
}
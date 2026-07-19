package com.retail.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "ProductUOM", uniqueConstraints = {
    @UniqueConstraint(name = "UQ_ProductUOM_Product_UomName", columnNames = {"ProductId", "UomName"}),
    @UniqueConstraint(name = "UQ_ProductUOM_Barcode", columnNames = {"Barcode"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductUOM {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "UomId")
    private Long uomId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProductId", nullable = false)
    private Product product;

    @Column(name = "UomName", nullable = false, length = 50)
    private String uomName;

    @Column(name = "IsBaseUnit", nullable = false)
    private Boolean isBaseUnit;

    @Column(name = "ConversionRate", nullable = false)
    private Integer conversionRate;

    @Column(name = "Barcode", unique = true, length = 100)
    private String barcode;

    @Builder.Default
    @Column(name = "StandardPrice", nullable = false, precision = 18, scale = 2)
    private BigDecimal standardPrice = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false, length = 20)
    private ProductUOMStatus status;

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = ProductUOMStatus.ACTIVE;
        }
        if (standardPrice == null) {
            standardPrice = BigDecimal.ZERO;
        }
        if (isBaseUnit == null) {
            isBaseUnit = false;
        }
    }
}
package com.retail.procurement;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "ProductUOM", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"ProductId", "UomName"})
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

    @Column(name = "ConversionRate", nullable = false, precision = 18, scale = 4)
    private BigDecimal conversionRate;

    @Column(name = "IsBaseUnit", nullable = false)
    private Boolean isBaseUnit;
}

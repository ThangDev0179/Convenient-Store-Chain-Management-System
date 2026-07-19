package com.retail.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "PromotionDetail", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"PromotionId", "ProductId"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PromotionDetailId")
    private Long promotionDetailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PromotionId", nullable = false)
    private Promotion promotion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProductId", nullable = false)
    private Product product;

    @Column(name = "DiscountType", nullable = false, length = 20)
    private String discountType; // Percentage | FixedAmount

    @Column(name = "DiscountValue", nullable = false, precision = 18, scale = 2)
    private BigDecimal discountValue;
}

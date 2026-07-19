package com.retail.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "PromotionDetail")
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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ProductId", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "DiscountType", nullable = false, length = 20)
    private DiscountType discountType;

    @Column(name = "DiscountValue", nullable = false, precision = 18, scale = 2)
    private BigDecimal discountValue;
}

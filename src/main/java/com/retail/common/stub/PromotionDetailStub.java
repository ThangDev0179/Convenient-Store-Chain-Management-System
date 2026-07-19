package com.retail.common.stub;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * STUB — sẽ được thay bằng entity chính thức của thành viên 2 khi merge.
 * DiscountType: Percentage | FixedAmount
 */
@Entity
@Table(name = "PromotionDetail")
@Getter
@Setter
@NoArgsConstructor
public class PromotionDetailStub {

    @Id
    @Column(name = "PromotionDetailId")
    private Long promotionDetailId;

    @Column(name = "PromotionId")
    private Long promotionId;

    @Column(name = "ProductId")
    private Long productId;

    @Column(name = "DiscountType", length = 20)
    private String discountType; // Percentage | FixedAmount

    @Column(name = "DiscountValue", precision = 18, scale = 2)
    private BigDecimal discountValue;

    // Convenience reference to parent (not eagerly loaded — lazy via FK)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PromotionId", insertable = false, updatable = false)
    private PromotionStub promotion;
}

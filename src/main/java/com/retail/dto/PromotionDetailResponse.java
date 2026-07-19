package com.retail.dto;

import com.retail.entity.DiscountType;
import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionDetailResponse {
    private Long promotionDetailId;
    private Long productId;
    private String productSku;
    private String productName;
    private DiscountType discountType;
    private BigDecimal discountValue;
}

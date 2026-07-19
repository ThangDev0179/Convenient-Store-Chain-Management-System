package com.retail.service;

import com.retail.entity.Promotion;
import com.retail.entity.PromotionDetail;
import java.math.BigDecimal;
import java.util.List;

public interface PromotionService {
    Promotion createPromotion(String name, String startStr, String endStr, Long createdByEmployeeId);
    Promotion addDetail(Long promotionId, Long productId, String discountType, BigDecimal discountValue);
    Promotion activatePromotion(Long promotionId);
    Promotion cancelPromotion(Long promotionId);
    PromotionDetail getBestActivePromotion(Long productId, BigDecimal currentPrice);
    List<Promotion> getAllPromotions();
}

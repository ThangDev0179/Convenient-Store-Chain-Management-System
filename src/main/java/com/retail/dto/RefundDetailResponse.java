package com.retail.dto;

import com.retail.entity.ConditionType;

import java.math.BigDecimal;

public record RefundDetailResponse(
        Long refundDetailId,
        Long productId,
        String sku,
        String productName,
        BigDecimal quantity,
        ConditionType conditionType,
        BigDecimal unitRefundAmount
) {}

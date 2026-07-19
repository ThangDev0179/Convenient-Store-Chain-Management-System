package com.retail.dto;

import com.retail.entity.ConditionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * One item in a refund request.
 * Rule #9: conditionType must be Damaged or Resalable (enum handles this).
 * Rule #11: quantity > 0.
 */
public record RefundItemRequest(

        @NotNull(message = "ProductId is required")
        Long productId,

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.001", message = "Quantity must be greater than 0")
        BigDecimal quantity,

        @NotNull(message = "Condition type is required (Damaged or Resalable)")
        ConditionType conditionType
) {}

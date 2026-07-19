package com.retail.dto;

import java.math.BigDecimal;

/**
 * Lightweight product search result for the POS product search endpoint.
 * GET /pos/products/search?keyword=...&sku=...
 * Returns effective price (BranchProductPrice if exists, else StandardPrice)
 * and real-time QtyAvailable from BranchInventory.
 */
public record ProductSearchResponse(
        Long productId,
        String sku,
        String productName,
        BigDecimal effectivePrice,   // BranchProductPrice.EffectivePrice || Product.StandardPrice
        BigDecimal qtyAvailable,     // BranchInventory.QtyAvailable — realtime stock
        Long activePromotionId,      // nullable: promotionId if there's an active promotion
        String discountType,         // nullable: Percentage | FixedAmount
        java.math.BigDecimal discountValue  // nullable
) {}

package com.retail.dto;

import java.math.BigDecimal;

/**
 * Response DTO for a single InvoiceDetail line.
 * productName and sku are joined from ProductStub in the mapper.
 */
public record InvoiceDetailResponse(
        Long invoiceDetailId,
        Long productId,
        String sku,
        String productName,
        BigDecimal quantity,
        BigDecimal unitPrice,
        Long promotionId,
        BigDecimal lineTotal,   // Quantity * UnitPrice — read from @Formula field
        BigDecimal qtyAvailable
) {}

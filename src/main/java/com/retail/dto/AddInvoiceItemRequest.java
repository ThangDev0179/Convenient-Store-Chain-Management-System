package com.retail.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Request body for POST /pos/invoices/{id}/items.
 * Rule #10: If productId already exists in the invoice, service will merge quantities.
 * Rule #11: quantity must be > 0.
 */
public record AddInvoiceItemRequest(

        @NotNull(message = "ProductId is required")
        Long productId,

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.001", message = "Quantity must be greater than 0")
        BigDecimal quantity
) {}

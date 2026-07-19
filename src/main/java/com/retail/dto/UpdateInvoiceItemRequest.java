package com.retail.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Request body for PUT /pos/invoices/{id}/items/{detailId}.
 * Only quantity can be changed — product cannot change (delete + add new instead).
 */
public record UpdateInvoiceItemRequest(

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.001", message = "Quantity must be greater than 0")
        BigDecimal quantity
) {}

package com.retail.dto;

import com.retail.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for PUT /pos/invoices/{id}/pay.
 * Rule #2: paymentMethod is mandatory and must be one of {Cash, QR, Bank, Card}.
 */
public record PayInvoiceRequest(

        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod
) {}

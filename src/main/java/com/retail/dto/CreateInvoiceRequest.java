package com.retail.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Request body for POST /pos/invoices.
 * BranchId and CashierId are resolved from the authenticated user's session,
 * NOT accepted from the client (security: prevents cashier spoofing).
 */
public record CreateInvoiceRequest(
        // No fields required from client — branch/cashier from security context
) {}

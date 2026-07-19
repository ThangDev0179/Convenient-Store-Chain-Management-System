package com.retail.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request body for POST /refunds.
 *
 * Security: originalInvoiceId provided by client; BranchId and RequestedBy
 * are resolved server-side (from invoice and security context respectively).
 *
 * Validation rules enforced here (Bean Validation):
 *   @NotBlank customerName, customerPhone, reason
 *   @Pattern  customerPhone — Vietnamese mobile number
 *   @Size     reason max 500 (matches NVARCHAR(500))
 *   @NotEmpty items
 */
public record CreateRefundRequest(

        @NotNull(message = "Original invoice ID is required")
        Long originalInvoiceId,

        @NotBlank(message = "Customer name is required")
        String customerName,

        @NotBlank(message = "Customer phone is required")
        @Pattern(regexp = "^(0|\\+84)[0-9]{9,10}$",
                 message = "Invalid Vietnamese phone number (e.g. 0912345678 or +84912345678)")
        String customerPhone,

        @NotBlank(message = "Reason is required")
        @Size(max = 500, message = "Reason must not exceed 500 characters")
        String reason,

        @NotEmpty(message = "At least one item is required")
        @Valid
        List<RefundItemRequest> items
) {}

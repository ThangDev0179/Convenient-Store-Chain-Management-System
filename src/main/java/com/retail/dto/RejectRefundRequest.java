package com.retail.dto;

import jakarta.validation.constraints.Size;

/**
 * Request body for PUT /refunds/{id}/reject.
 */
public record RejectRefundRequest(
        @Size(max = 500, message = "Rejection reason must not exceed 500 characters")
        String reason
) {}

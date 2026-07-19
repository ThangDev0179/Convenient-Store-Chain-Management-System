package com.retail.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for PUT /refunds/{id}/override-approve.
 * Manager physically at POS enters their own credentials to bypass async approval flow.
 * managerPin is authenticated against the Manager's existing password (BCrypt).
 * No separate PIN table needed.
 */
public record RefundOverrideApproveRequest(

        @NotBlank(message = "Manager username is required")
        String managerUsername,

        @NotBlank(message = "Manager PIN/password is required")
        String managerPin
) {}

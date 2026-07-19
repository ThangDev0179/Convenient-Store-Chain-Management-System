package com.retail.dto;

import com.retail.entity.RefundStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record RefundResponse(
        Long refundId,
        String refundCode,
        Long originalInvoiceId,
        String originalInvoiceCode,   // joined from Invoice
        Integer branchId,
        String customerName,
        String customerPhone,
        String reason,
        BigDecimal totalRefundAmount,
        RefundStatus status,
        Long requestedBy,
        String requestedByName,        // joined from EmployeeStub
        Long approvedBy,
        String approvedByName,         // joined from EmployeeStub (nullable)
        boolean pinOverrideUsed,
        LocalDateTime createdAt,
        LocalDateTime approvedAt,
        List<RefundDetailResponse> items
) {}

package com.retail.dto;

import com.retail.entity.InvoiceStatus;
import com.retail.entity.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Full Invoice response DTO.
 * cashierName is joined from EmployeeStub in the mapper.
 */
public record InvoiceResponse(
        Long invoiceId,
        String invoiceCode,
        Integer branchId,
        Long cashierId,
        String cashierName,        // join from EmployeeStub
        InvoiceStatus status,
        PaymentMethod paymentMethod,
        BigDecimal totalAmount,
        LocalDateTime createdAt,
        LocalDateTime paidAt,
        LocalDateTime canceledAt,
        List<InvoiceDetailResponse> items
) {}

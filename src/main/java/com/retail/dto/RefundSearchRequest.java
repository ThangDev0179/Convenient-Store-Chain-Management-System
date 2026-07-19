package com.retail.dto;

import com.retail.entity.RefundStatus;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Query parameter binding for GET /refunds list endpoint.
 */
public record RefundSearchRequest(
        RefundStatus status,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate fromDate,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate toDate,

        Integer branchId,

        Integer page,

        Integer size
) {
    public RefundSearchRequest {
        if (page == null || page < 0) page = 0;
        if (size == null || size <= 0) size = 20;
    }
}

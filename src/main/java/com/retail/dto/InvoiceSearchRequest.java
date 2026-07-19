package com.retail.dto;

import com.retail.entity.InvoiceStatus;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Query parameter binding object for GET /pos/invoices.
 * Used with @ModelAttribute in Controller for clean URL binding.
 */
public record InvoiceSearchRequest(
        InvoiceStatus status,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate fromDate,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate toDate,

        Long cashierId,

        int page,

        int size,

        String sort   // e.g., "createdAt,desc"
) {
    public InvoiceSearchRequest {
        if (page < 0) page = 0;
        if (size <= 0) size = 20;
        if (sort == null || sort.isBlank()) sort = "createdAt,desc";
    }
}

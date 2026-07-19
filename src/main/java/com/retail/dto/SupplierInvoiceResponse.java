package com.retail.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierInvoiceResponse {
    private Long supplierInvoiceId;
    private Long grnId;
    private String grnCode;
    private Integer supplierId;
    private String supplierName;
    private BigDecimal amount;
    private BigDecimal amountPaid;
    private String status;
    private LocalDateTime issuedAt;
    private Long approvedById;
    private String approvedByName;
    private LocalDateTime approvedAt;
}

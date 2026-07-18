package com.retail.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierPaymentResponse {
    private Long supplierPaymentId;
    private Long supplierInvoiceId;
    private String grnCode;
    private String supplierName;
    private BigDecimal amountPaid;
    private String paymentMethod;
    private Long paidById;
    private String paidByName;
    private LocalDateTime paidAt;
}

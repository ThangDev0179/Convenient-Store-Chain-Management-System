package com.retail.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSupplierPaymentRequest {

    @NotNull(message = "ID hóa đơn không được để trống")
    private Long supplierInvoiceId;

    @NotNull(message = "Số tiền thanh toán không được để trống")
    @DecimalMin(value = "0.01", message = "Số tiền thanh toán phải lớn hơn 0")
    private BigDecimal amountPaid;

    @NotBlank(message = "Phương thức thanh toán không được để trống")
    @jakarta.validation.constraints.Pattern(regexp = "^(Cash|Bank|QR|Card)$", message = "Phương thức thanh toán phải là Cash, Bank, QR hoặc Card")
    private String paymentMethod; // Cash, Bank, QR, Card
}

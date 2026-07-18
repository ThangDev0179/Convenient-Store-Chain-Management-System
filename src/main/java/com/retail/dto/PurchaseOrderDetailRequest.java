package com.retail.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderDetailRequest {

    @NotNull(message = "Sản phẩm không được để trống")
    private Long productId;

    @NotNull(message = "Đơn vị tính không được để trống")
    private Long uomId;

    @NotNull(message = "Số lượng đặt không được để trống")
    @DecimalMin(value = "0.001", message = "Số lượng đặt phải lớn hơn 0")
    private BigDecimal quantityOrdered;

    @NotNull(message = "Giá nhập không được để trống")
    @DecimalMin(value = "0.00", message = "Giá nhập phải lớn hơn hoặc bằng 0")
    private BigDecimal unitCost;
}

package com.retail.procurement.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsReceiptNoteDetailRequest {

    @NotNull(message = "Sản phẩm không được trống")
    private Long productId;

    @NotNull(message = "Đơn vị tính không được trống")
    private Long uomId;

    @NotNull(message = "Số lượng yêu cầu đặt không được trống")
    private BigDecimal quantityOrdered;

    @NotNull(message = "Số lượng thực nhận không được trống")
    @DecimalMin(value = "0.000", inclusive = false, message = "Số lượng thực nhận phải lớn hơn 0")
    private BigDecimal quantityReceived;

    @NotNull(message = "Đơn giá thực tế không được trống")
    @DecimalMin(value = "0.00", message = "Đơn giá không được âm")
    private BigDecimal unitCost;
}

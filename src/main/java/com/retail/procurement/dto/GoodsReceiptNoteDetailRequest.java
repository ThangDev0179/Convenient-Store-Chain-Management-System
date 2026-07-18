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

    @NotNull(message = "Số lượng thực nhận không được trống")
    @DecimalMin(value = "0.000", inclusive = false, message = "Số lượng thực nhận phải lớn hơn 0")
    private BigDecimal quantityReceived;
}

package com.retail.transfer.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ReceiveTransferRequest {

    @NotEmpty(message = "Danh sách xác nhận nhận hàng không được trống")
    @Valid
    private List<ReceiveDetailDto> details;

    @Data
    public static class ReceiveDetailDto {

        @NotNull(message = "Vui lòng chọn sản phẩm chi tiết")
        private Long transferDetailId;

        @NotNull(message = "Số lượng thực nhận không được trống")
        @PositiveOrZero(message = "Số lượng thực nhận phải >= 0")
        private BigDecimal quantityReceived;
    }
}

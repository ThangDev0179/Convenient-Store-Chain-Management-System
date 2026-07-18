package com.retail.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class StockTransferRequest {

    @NotNull(message = "Vui lòng chọn chi nhánh gửi")
    private Integer fromBranchId;

    @NotNull(message = "Vui lòng chọn chi nhánh nhận")
    private Integer toBranchId;

    @NotEmpty(message = "Danh sách sản phẩm không được trống")
    @Valid
    private List<TransferDetailDto> details;

    @Data
    public static class TransferDetailDto {

        @NotNull(message = "Vui lòng chọn sản phẩm")
        private Long productId;

        @NotNull(message = "Số lượng gửi không được trống")
        @Positive(message = "Số lượng gửi phải lớn hơn 0")
        private BigDecimal quantitySent;
    }
}
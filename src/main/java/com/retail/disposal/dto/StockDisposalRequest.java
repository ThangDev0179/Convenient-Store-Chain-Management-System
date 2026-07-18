package com.retail.disposal.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class StockDisposalRequest {

    @NotNull(message = "Vui lòng chọn chi nhánh")
    private Integer branchId;

    private String reason;

    @NotEmpty(message = "Danh sách sản phẩm hủy không được trống")
    @Valid
    private List<DisposalDetailDto> details;

    @Data
    public static class DisposalDetailDto {

        @NotNull(message = "Vui lòng chọn sản phẩm")
        private Long productId;

        @NotNull(message = "Số lượng hủy không được trống")
        @Positive(message = "Số lượng hủy phải lớn hơn 0")
        private BigDecimal quantityDisposed;

        private String note;
    }
}

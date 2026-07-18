package com.retail.procurement.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateGoodsReceiptNoteRequest {

    @NotNull(message = "Đơn đặt hàng liên kết không được trống")
    private Long purchaseOrderId;

    @NotNull(message = "Chi nhánh thực hiện nhập kho không được trống")
    private Integer branchId;

    @NotEmpty(message = "Danh sách chi tiết nhập kho không được trống")
    @Valid
    private List<GoodsReceiptNoteDetailRequest> details;
}

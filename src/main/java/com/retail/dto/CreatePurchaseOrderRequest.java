package com.retail.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePurchaseOrderRequest {

    @NotNull(message = "Nhà cung cấp không được để trống")
    private Integer supplierId;

    @NotNull(message = "Chi nhánh không được để trống")
    private Integer branchId;

    @NotEmpty(message = "Chi tiết đơn đặt hàng không được để trống")
    @Valid
    private List<PurchaseOrderDetailRequest> details;
}

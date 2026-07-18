package com.retail.dto;
import com.retail.entity.Branch;
import com.retail.entity.Product;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class InventoryCountRequest {
    @NotNull(message = "Branch ID is required")
    private Integer branchId;

    @NotEmpty(message = "Details cannot be empty")
    private List<InventoryCountDetailDto> details;

    @Data
    public static class InventoryCountDetailDto {
        @NotNull(message = "Product ID is required")
        private Long productId;

        @NotNull(message = "Actual Quantity is required")
        private BigDecimal actualQty;
    }
}
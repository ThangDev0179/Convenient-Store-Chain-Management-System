package com.retail.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchInventoryResponse {
    private Integer branchId;
    private String branchName;
    private Long productId;
    private String productSku;
    private String productName;
    private String categoryName;
    private BigDecimal qtyOnHand;
    private BigDecimal qtyAvailable;
    private BigDecimal qtyInTransit;
    private LocalDateTime updatedAt;
}

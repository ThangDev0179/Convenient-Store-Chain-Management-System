package com.retail.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderDetailResponse {
    private Long poDetailId;
    private Long productId;
    private String sku;
    private String productName;
    private Long uomId;
    private String uomName;
    private BigDecimal quantityOrdered;
    private BigDecimal unitCost;
    private BigDecimal lineTotal;
}

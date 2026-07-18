package com.retail.procurement.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsReceiptNoteDetailResponse {
    private Long grnDetailId;
    private Long productId;
    private String productSku;
    private String productName;
    private Long uomId;
    private String uomName;
    private BigDecimal quantityOrdered;
    private BigDecimal quantityReceived;
    private BigDecimal unitCost;
    private BigDecimal totalCost;
}

package com.retail.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderResponse {
    private Long purchaseOrderId;
    private String poCode;
    private Integer branchId;
    private String branchName;
    private Integer supplierId;
    private String supplierName;
    private String status;
    private Long createdById;
    private String createdByName;
    private LocalDateTime createdAt;
    private List<PurchaseOrderDetailResponse> details;
    private BigDecimal totalCost;
}
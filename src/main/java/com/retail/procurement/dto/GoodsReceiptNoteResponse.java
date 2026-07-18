package com.retail.procurement.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsReceiptNoteResponse {
    private Long grnId;
    private String grnCode;
    private Long purchaseOrderId;
    private String purchaseOrderCode;
    private Integer branchId;
    private String branchName;
    private LocalDateTime receivedDate;
    private Long receivedById;
    private String receivedByName;
    private String status;
    private BigDecimal totalCost;
    private LocalDateTime createdAt;
    private List<GoodsReceiptNoteDetailResponse> details;
}

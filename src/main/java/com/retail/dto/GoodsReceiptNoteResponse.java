package com.retail.dto;

import lombok.*;
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
    private LocalDateTime receivedAt;
    private Long receivedById;
    private String receivedByName;
    private String status;
    private LocalDateTime createdAt;
    private List<GoodsReceiptNoteDetailResponse> details;
}
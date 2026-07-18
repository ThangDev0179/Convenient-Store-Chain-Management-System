package com.retail.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryTransactionResponse {
    private Long transactionId;
    private Integer branchId;
    private String branchName;
    private Long productId;
    private String productSku;
    private String productName;
    private String transactionType;
    private String referenceTable;
    private Long referenceId;
    private BigDecimal quantityChange;
    private String reason;
    private String createdByName;
    private LocalDateTime createdAt;
}

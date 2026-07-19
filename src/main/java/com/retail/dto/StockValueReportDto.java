package com.retail.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockValueReportDto {
    private Integer branchId;
    private String branchName;
    private BigDecimal totalQtyOnHand;
    private BigDecimal totalQtyAvailable;
    private BigDecimal totalValue;
}

package com.retail.dto;

import com.retail.entity.DisposalSourceType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LossReportDto {
    private Integer branchId;
    private String branchName;
    private DisposalSourceType sourceType;
    private BigDecimal totalDisposedQty;
    private BigDecimal totalLossValue;
}

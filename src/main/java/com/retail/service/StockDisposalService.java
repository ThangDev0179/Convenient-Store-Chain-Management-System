package com.retail.service;
import com.retail.entity.DisposalSourceType;
import com.retail.entity.StockDisposal;
import com.retail.dto.StockDisposalRequest;


import java.math.BigDecimal;
import java.util.List;

public interface StockDisposalService {
    StockDisposal createManualDisposal(StockDisposalRequest request, Long createdByEmployeeId);
    
    StockDisposal autoCreateFromLoss(Integer branchId, Long productId, BigDecimal lossQty, 
                                     DisposalSourceType type, Long refId, String reason, Long employeeId);
                                     
    StockDisposal approveDisposal(Long disposalId, Long approvedByEmployeeId);
    
    StockDisposal rejectDisposal(Long disposalId, Long rejectedByEmployeeId);
    
    List<StockDisposal> getAllDisposals();
    
    StockDisposal getDisposalById(Long disposalId);
}
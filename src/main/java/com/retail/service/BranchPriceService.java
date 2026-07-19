package com.retail.service;

import com.retail.entity.BranchPriceRequest;
import com.retail.entity.BranchProductPrice;
import java.math.BigDecimal;
import java.util.List;

public interface BranchPriceService {
    BranchPriceRequest createRequest(Integer branchId, Long productId, BigDecimal proposedPrice, Long employeeId);
    BranchPriceRequest approveRequest(Long requestId, Long employeeId);
    BranchPriceRequest rejectRequest(Long requestId, Long employeeId);
    BigDecimal getEffectivePrice(Integer branchId, Long productId);
    List<BranchPriceRequest> getRequestsByBranch(Integer branchId);
    List<BranchPriceRequest> getAllRequests();
}

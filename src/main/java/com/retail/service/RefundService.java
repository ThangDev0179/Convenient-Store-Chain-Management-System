package com.retail.service;

import com.retail.entity.Refund;
import com.retail.entity.RefundDetail;
import java.math.BigDecimal;
import java.util.List;

public interface RefundService {
    Refund createRefund(Long originalInvoiceId, String customerName, String customerPhone, String reason, Long cashierId);
    Refund addDetail(Long refundId, Long productId, BigDecimal quantity, String conditionType);
    Refund approveRefund(Long refundId, Long managerEmployeeId, String managerPinOverride);
    Refund getDetail(Long refundId);
    List<Refund> getRefundsByBranch(Integer branchId);
}

package com.retail.mapper;

import com.retail.common.stub.ProductStub;
import com.retail.dto.RefundDetailResponse;
import com.retail.dto.RefundResponse;
import com.retail.entity.Refund;
import com.retail.entity.RefundDetail;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Manual mapper for Refund module.
 *
 * Integration notes:
 *   - originalInvoiceCode  → passed as String from service (joined via InvoiceRepository)
 *   - requestedByName/approvedByName → passed as String from service (joined via EmployeeRepository)
 *   - sku/productName → joined from ProductStub (replace with real Product at full team merge)
 */
@Component
public class RefundMapper {

    public RefundDetailResponse toDetailResponse(RefundDetail detail,
                                                  Map<Long, ProductStub> productMap) {
        ProductStub p = productMap.get(detail.getProductId());
        return new RefundDetailResponse(
                detail.getRefundDetailId(),
                detail.getProductId(),
                p != null ? p.getSku() : "N/A",
                p != null ? p.getProductName() : "Unknown",
                detail.getQuantity(),
                detail.getConditionType(),
                detail.getUnitRefundAmount()
        );
    }

    public RefundResponse toResponse(Refund refund,
                                      String originalInvoiceCode,
                                      Map<Long, ProductStub> productMap,
                                      String requestedByName,
                                      String approvedByName) {
        List<RefundDetailResponse> items = refund.getDetails().stream()
                .map(d -> toDetailResponse(d, productMap))
                .toList();

        return new RefundResponse(
                refund.getRefundId(),
                refund.getRefundCode(),
                refund.getOriginalInvoiceId(),
                originalInvoiceCode,
                refund.getBranchId(),
                refund.getCustomerName(),
                refund.getCustomerPhone(),
                refund.getReason(),
                refund.getTotalRefundAmount(),
                refund.getStatus(),
                refund.getRequestedBy(),
                requestedByName != null ? requestedByName : "N/A",
                refund.getApprovedBy(),
                approvedByName,
                refund.isPinOverrideUsed(),
                refund.getCreatedAt(),
                refund.getApprovedAt(),
                items
        );
    }

    /** Summary mapper for list view (no items loaded) */
    public RefundResponse toSummaryResponse(Refund refund,
                                             String originalInvoiceCode,
                                             String requestedByName,
                                             String approvedByName) {
        return new RefundResponse(
                refund.getRefundId(),
                refund.getRefundCode(),
                refund.getOriginalInvoiceId(),
                originalInvoiceCode,
                refund.getBranchId(),
                refund.getCustomerName(),
                refund.getCustomerPhone(),
                refund.getReason(),
                refund.getTotalRefundAmount(),
                refund.getStatus(),
                refund.getRequestedBy(),
                requestedByName != null ? requestedByName : "N/A",
                refund.getApprovedBy(),
                approvedByName,
                refund.isPinOverrideUsed(),
                refund.getCreatedAt(),
                refund.getApprovedAt(),
                List.of()
        );
    }
}

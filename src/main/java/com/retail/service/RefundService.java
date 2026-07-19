package com.retail.service;

import com.retail.dto.*;
import com.retail.dto.RefundResponse;
import org.springframework.data.domain.Page;

public interface RefundService {

    /** 3.2.1 — Tạo yêu cầu đổi trả */
    RefundResponse createRefund(CreateRefundRequest request);

    /** 3.2.2 — MANAGER/ADMIN: Phê duyệt refund */
    RefundResponse approveRefund(Long refundId);

    /** 3.2.2 — MANAGER/ADMIN: Từ chối refund */
    RefundResponse rejectRefund(Long refundId, String rejectionReason);

    /** 3.2.2 — Manager PIN override tại POS */
    RefundResponse overrideApprove(Long refundId, RefundOverrideApproveRequest request);

    /** 3.2.4 — Danh sách refund có filter + phân trang */
    Page<RefundResponse> listRefunds(RefundSearchRequest request);

    /** 3.2.4 — Chi tiết refund */
    RefundResponse getRefundDetail(Long refundId);
}

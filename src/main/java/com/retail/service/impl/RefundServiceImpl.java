package com.retail.service.impl;

import com.retail.entity.*;
import com.retail.exception.ValidationException;
import com.retail.repository.RefundDetailRepository;
import com.retail.repository.RefundRepository;
import com.retail.repository.InvoiceRepository;
import com.retail.service.RefundService;
import com.retail.service.InventoryTransactionService;
import com.retail.service.AuditLogService;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {

    private final RefundRepository refundRepository;
    private final RefundDetailRepository refundDetailRepository;
    private final InvoiceRepository invoiceRepository;
    private final InventoryTransactionService transactionService;
    private final AuditLogService auditLogService;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public Refund createRefund(Long originalInvoiceId, String customerName, String customerPhone, String reason, Long cashierId) {
        Invoice invoice = invoiceRepository.findById(originalInvoiceId)
                .orElseThrow(() -> new ValidationException("Hóa đơn gốc không tồn tại"));

        if (!"Paid".equals(invoice.getStatus())) {
            throw new ValidationException("Chỉ được đổi trả sản phẩm từ hóa đơn đã thanh toán (Paid)");
        }

        // Kiểm tra thời hạn 7 ngày
        LocalDateTime now = LocalDateTime.now();
        if (invoice.getPaidAt() == null || now.isAfter(invoice.getPaidAt().plusDays(7))) {
            throw new ValidationException("Đã quá thời hạn đổi trả 7 ngày cho hóa đơn này (Ngày thanh toán: " + invoice.getPaidAt() + ")");
        }

        Branch branch = invoice.getBranch();

        // Generate RefundCode: REF-[BranchCode]-YYYYMMDD-[4 số]
        LocalDate today = LocalDate.now();
        String dateStr = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "REF-" + branch.getBranchCode() + "-" + dateStr + "-";

        String maxCode = refundRepository.findMaxRefundCodeByBranchAndDate(branch.getBranchCode(), dateStr);
        int nextSeq = 1;
        if (maxCode != null && maxCode.startsWith(prefix) && maxCode.length() == prefix.length() + 4) {
            try {
                String seqStr = maxCode.substring(prefix.length());
                nextSeq = Integer.parseInt(seqStr) + 1;
            } catch (NumberFormatException ignored) {}
        }
        String refundCode = prefix + String.format("%04d", nextSeq);

        Refund refund = Refund.builder()
                .refundCode(refundCode)
                .originalInvoice(invoice)
                .branch(branch)
                .customerName(customerName.trim())
                .customerPhone(customerPhone.trim())
                .reason(reason.trim())
                .totalRefundAmount(BigDecimal.ZERO)
                .status("Draft")
                .requestedBy(entityManager.getReference(Employee.class, cashierId))
                .build();

        Refund saved = refundRepository.save(refund);
        auditLogService.logAction(cashierId, "CreateRefund", "Refund",
                saved.getRefundId(), null, "Draft", "Tạo phiếu đổi trả nháp", null, null);
        return saved;
    }

    @Override
    @Transactional
    public Refund addDetail(Long refundId, Long productId, BigDecimal quantity, String conditionType) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new ValidationException("Phiếu đổi trả không tồn tại"));

        if (!"Draft".equals(refund.getStatus())) {
            throw new ValidationException("Chỉ được chỉnh sửa phiếu đổi trả ở trạng thái Draft");
        }

        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Số lượng đổi trả phải lớn hơn 0");
        }

        if (!"Damaged".equals(conditionType) && !"Resalable".equals(conditionType)) {
            throw new ValidationException("Trạng thái sản phẩm đổi trả không hợp lệ (Damaged | Resalable)");
        }

        Product product = entityManager.find(Product.class, productId);
        if (product == null) {
            throw new ValidationException("Sản phẩm không tồn tại");
        }

        // Tìm sản phẩm trong hóa đơn gốc
        InvoiceDetail origDetail = refund.getOriginalInvoice().getDetails().stream()
                .filter(d -> d.getProduct().getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new ValidationException("Sản phẩm '" + product.getProductName() + "' không tồn tại trong hóa đơn gốc"));

        // Kiểm tra số lượng đổi trả lũy kế
        BigDecimal alreadyRefunded = refundDetailRepository.sumRefundedQtyForProduct(refund.getOriginalInvoice().getInvoiceId(), productId);
        BigDecimal totalRequested = alreadyRefunded.add(quantity);

        if (totalRequested.compareTo(origDetail.getQuantity()) > 0) {
            throw new ValidationException("Tổng số lượng đổi trả lũy kế (" + totalRequested + ") vượt quá số lượng mua ban đầu (" + origDetail.getQuantity() + ") cho sản phẩm: " + product.getProductName());
        }

        RefundDetail detail = RefundDetail.builder()
                .refund(refund)
                .product(product)
                .quantity(quantity)
                .conditionType(conditionType)
                .unitRefundAmount(origDetail.getUnitPrice())
                .build();

        refund.addDetail(detail);

        // Tính lại tổng tiền hoàn trả
        BigDecimal lineRefund = quantity.multiply(origDetail.getUnitPrice());
        BigDecimal currentTotal = refund.getTotalRefundAmount() != null ? refund.getTotalRefundAmount() : BigDecimal.ZERO;
        refund.setTotalRefundAmount(currentTotal.add(lineRefund));

        // Tự động chuyển trạng thái chờ duyệt nếu số tiền >= 200,000 VNĐ
        if (refund.getTotalRefundAmount().compareTo(new BigDecimal("200000.00")) >= 0) {
            refund.setStatus("Pending_Approval");
        } else {
            refund.setStatus("Draft");
        }

        return refundRepository.save(refund);
    }

    @Override
    @Transactional
    public Refund approveRefund(Long refundId, Long managerEmployeeId, String managerPinOverride) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new ValidationException("Phiếu đổi trả không tồn tại"));

        BigDecimal limit = new BigDecimal("200000.00");
        boolean requiresApproval = refund.getTotalRefundAmount().compareTo(limit) >= 0;

        if (requiresApproval) {
            if (managerEmployeeId == null || managerPinOverride == null || managerPinOverride.trim().isEmpty()) {
                throw new ValidationException("Số tiền hoàn trả từ 200,000 VNĐ trở lên yêu cầu Quản lý nhập mã PIN phê duyệt trực tiếp.");
            }

            Employee manager = entityManager.find(Employee.class, managerEmployeeId);
            if (manager == null || !"MANAGER".equals(manager.getRole().getRoleCode().name())) {
                throw new ValidationException("Nhân viên phê duyệt không phải là Quản lý chi nhánh");
            }

            if (manager.getPinCode() == null || !manager.getPinCode().equals(managerPinOverride.trim())) {
                throw new ValidationException("Mã PIN phê duyệt của Quản lý không chính xác");
            }

            refund.setApprovedBy(manager);
            refund.setPinOverrideUsed(true);
        }

        Integer branchId = refund.getBranch().getBranchId();

        // Hoàn trả kho bãi tương ứng cho từng sản phẩm
        for (RefundDetail detail : refund.getDetails()) {
            Long productId = detail.getProduct().getProductId();
            BigDecimal qty = detail.getQuantity();

            if ("Resalable".equals(detail.getConditionType())) {
                // Hàng nguyên vẹn: cộng lại kho khả dụng bán hàng và kho vật lý
                transactionService.recordTransaction(
                        branchId, productId,
                        qty, qty, BigDecimal.ZERO,
                        InventoryTransactionType.Refund_Restock,
                        "Refund", refundId,
                        "Đổi trả hoàn kho (nguyên vẹn): " + refund.getRefundCode(),
                        refund.getRequestedBy().getEmployeeId()
                );
            } else if ("Damaged".equals(detail.getConditionType())) {
                // Hàng lỗi/hỏng: cộng kho vật lý (chờ thanh lý), không cộng kho khả dụng
                transactionService.recordTransaction(
                        branchId, productId,
                        qty, BigDecimal.ZERO, BigDecimal.ZERO,
                        InventoryTransactionType.Refund_Restock,
                        "Refund", refundId,
                        "Đổi trả hoàn kho (lỗi/chờ hủy): " + refund.getRefundCode(),
                        refund.getRequestedBy().getEmployeeId()
                );
            }
        }

        refund.setStatus("Completed");
        refund.setApprovedAt(LocalDateTime.now());
        Refund saved = refundRepository.save(refund);

        auditLogService.logAction(refund.getRequestedBy().getEmployeeId(), "ApproveRefund", "Refund",
                saved.getRefundId(), "Pending_Approval", "Completed", "Hoàn tất phiếu đổi trả hoàn tiền", null, null);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Refund getDetail(Long refundId) {
        return refundRepository.findById(refundId)
                .orElseThrow(() -> new ValidationException("Phiếu đổi trả không tồn tại"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Refund> getRefundsByBranch(Integer branchId) {
        return refundRepository.findAll().stream()
                .filter(r -> r.getBranch().getBranchId().equals(branchId))
                .toList();
    }
}

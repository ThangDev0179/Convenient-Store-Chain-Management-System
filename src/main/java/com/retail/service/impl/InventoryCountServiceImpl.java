package com.retail.service.impl;
import com.retail.service.AuditLogService;
import com.retail.entity.Branch;
import com.retail.entity.BranchInventory;
import com.retail.repository.BranchInventoryRepository;
import com.retail.entity.DisposalSourceType;
import com.retail.entity.Employee;
import com.retail.entity.InventoryCount;
import com.retail.entity.InventoryCountDetail;
import com.retail.repository.InventoryCountRepository;
import com.retail.dto.InventoryCountRequest;
import com.retail.service.InventoryCountService;
import com.retail.entity.InventoryCountStatus;
import com.retail.service.InventoryTransactionService;
import com.retail.entity.InventoryTransactionType;
import com.retail.entity.Product;
import com.retail.service.StockDisposalService;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryCountServiceImpl implements InventoryCountService {

    private final InventoryCountRepository countRepository;
    private final BranchInventoryRepository branchInventoryRepository;
    private final InventoryTransactionService transactionService;
    private final StockDisposalService disposalService;
    private final AuditLogService auditLogService;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public InventoryCount createDraftCount(InventoryCountRequest request, Long createdByEmployeeId) {
        Branch branch = entityManager.find(Branch.class, request.getBranchId());
        if (branch == null) {
            throw new IllegalArgumentException("Chi nhánh không tồn tại");
        }

        // Generate CountCode: STK-[Mã Chi Nhánh]-YYYYMMDD-[4 số tăng tự động]
        java.time.LocalDate today = java.time.LocalDate.now();
        String dateStr = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "STK-" + branch.getBranchCode() + "-" + dateStr + "-";

        java.time.LocalDateTime startOfDay = today.atStartOfDay();
        List<InventoryCount> todayCounts = countRepository.findByBranchBranchIdAndCreatedAtAfter(request.getBranchId(), startOfDay);

        int nextSeq = 1;
        for (InventoryCount c : todayCounts) {
            String countCode = c.getCountCode();
            if (countCode != null && countCode.startsWith(prefix)) {
                try {
                    String seqStr = countCode.substring(prefix.length());
                    int seq = Integer.parseInt(seqStr);
                    if (seq >= nextSeq) {
                        nextSeq = seq + 1;
                    }
                } catch (Exception ignored) {}
            }
        }
        String code = prefix + String.format("%04d", nextSeq);

        InventoryCount count = new InventoryCount();
        count.setCountCode(code);
        count.setBranch(branch);
        count.setStatus(InventoryCountStatus.Draft);
        count.setCreatedBy(entityManager.getReference(Employee.class, createdByEmployeeId));


        for (InventoryCountRequest.InventoryCountDetailDto detailDto : request.getDetails()) {
            InventoryCountDetail detail = new InventoryCountDetail();
            detail.setProduct(entityManager.getReference(Product.class, detailDto.getProductId()));

            // BUG 2 FIX: Dùng QtyOnHand (số lượng vật lý thực tế) làm systemQty khi kiểm kê
            // vì kiểm kê là đếm hàng vật lý, không phân biệt hàng đang cam kết chuyển đi
            BigDecimal systemQty = branchInventoryRepository
                    .findByBranchBranchIdAndProductProductId(request.getBranchId(), detailDto.getProductId())
                    .map(BranchInventory::getQtyOnHand)
                    .orElse(BigDecimal.ZERO);

            detail.setSystemQty(systemQty);
            detail.setActualQty(detailDto.getActualQty());
            count.addDetail(detail);
        }

        InventoryCount saved = countRepository.save(count);
        auditLogService.logAction(createdByEmployeeId, "CreateInventoryCount", "InventoryCount",
                saved.getInventoryCountId(), null, saved.getStatus().name(), "Tạo phiếu kiểm kê nháp", null, null);
        return saved;
    }

    @Override
    @Transactional
    public InventoryCount submitCount(Long countId, Long employeeId) {
        InventoryCount count = getCountById(countId);
        if (count.getStatus() != InventoryCountStatus.Draft) {
            throw new IllegalStateException("Chỉ phiếu ở trạng thái Draft mới được gửi duyệt");
        }
        String oldStatus = count.getStatus().name();
        count.setStatus(InventoryCountStatus.Submitted);
        count.setSubmittedAt(LocalDateTime.now());

        InventoryCount saved = countRepository.save(count);
        auditLogService.logAction(employeeId, "SubmitInventoryCount", "InventoryCount",
                saved.getInventoryCountId(), oldStatus, saved.getStatus().name(), "Gửi duyệt", null, null);
        return saved;
    }

    @Override
    @Transactional
    public InventoryCount approveCount(Long countId, Long approvedByEmployeeId) {
        InventoryCount count = getCountById(countId);
        if (count.getStatus() != InventoryCountStatus.Submitted) {
            throw new IllegalStateException("Chỉ phiếu đã gửi mới được phê duyệt");
        }

        String oldStatus = count.getStatus().name();
        count.setStatus(InventoryCountStatus.Approved);
        count.setApprovedBy(entityManager.getReference(Employee.class, approvedByEmployeeId));
        count.setApprovedAt(LocalDateTime.now());

        for (InventoryCountDetail detail : count.getDetails()) {
            BigDecimal delta = detail.getActualQty().subtract(detail.getSystemQty());
            if (delta.compareTo(BigDecimal.ZERO) != 0) {
                transactionService.recordTransaction(
                        count.getBranch().getBranchId(),
                        detail.getProduct().getProductId(),
                        delta, delta, BigDecimal.ZERO,
                        InventoryTransactionType.CountAdjustment,
                        "InventoryCount", count.getInventoryCountId(),
                        "Phê duyệt kiểm kê: " + count.getCountCode(),
                        approvedByEmployeeId
                );
                
                // Nếu delta < 0 (thiếu hàng), tự động sinh phiếu Xuất Hủy (Disposal) để ghi nhận chi phí/báo cáo
                if (delta.compareTo(BigDecimal.ZERO) < 0) {
                    disposalService.autoCreateFromLoss(count.getBranch().getBranchId(), 
                            detail.getProduct().getProductId(), delta.negate(), 
                            DisposalSourceType.CountVariance, count.getInventoryCountId(), 
                            "Hụt kho qua kiểm kê: " + count.getCountCode(), approvedByEmployeeId);
                }
            }
        }

        InventoryCount saved = countRepository.save(count);
        auditLogService.logAction(approvedByEmployeeId, "ApproveInventoryCount", "InventoryCount",
                saved.getInventoryCountId(), oldStatus, saved.getStatus().name(),
                "Phê duyệt và cập nhật tồn kho", null, null);
        return saved;
    }

    @Override
    @Transactional
    public InventoryCount rejectCount(Long countId, Long rejectedByEmployeeId) {
        InventoryCount count = getCountById(countId);
        if (count.getStatus() != InventoryCountStatus.Submitted) {
            throw new IllegalStateException("Chỉ phiếu đã gửi mới được từ chối");
        }
        String oldStatus = count.getStatus().name();
        count.setStatus(InventoryCountStatus.Rejected);

        InventoryCount saved = countRepository.save(count);
        auditLogService.logAction(rejectedByEmployeeId, "RejectInventoryCount", "InventoryCount",
                saved.getInventoryCountId(), oldStatus, saved.getStatus().name(), "Từ chối", null, null);
        return saved;
    }

    @Override
    @Transactional
    public void cancelCount(Long countId, Long cancelledByEmployeeId) {
        InventoryCount count = getCountById(countId);
        if (count.getStatus() != InventoryCountStatus.Draft) {
            throw new IllegalStateException("Chỉ có thể hủy bỏ phiếu ở trạng thái Nháp (Draft)");
        }
        auditLogService.logAction(cancelledByEmployeeId, "CancelInventoryCount", "InventoryCount",
                countId, count.getStatus().name(), "Cancelled", "Hủy bỏ bản nháp kiểm kê", null, null);
        countRepository.delete(count);
    }

    @Override
    public List<InventoryCount> getAllCounts() {
        return countRepository.findAll();
    }

    @Override
    public InventoryCount getCountById(Long countId) {
        return countRepository.findById(countId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu kiểm kê ID: " + countId));
    }
}
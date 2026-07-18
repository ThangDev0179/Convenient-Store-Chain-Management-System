package com.retail.inventorycount.service.impl;

import com.retail.audit.service.AuditLogService;
import com.retail.branch.Branch;
import com.retail.employee.Employee;
import com.retail.inventory.service.InventoryTransactionService;
import com.retail.inventorycount.dto.InventoryCountRequest;
import com.retail.inventorycount.entity.InventoryCount;
import com.retail.inventorycount.entity.InventoryCountDetail;
import com.retail.inventorycount.entity.InventoryCountStatus;
import com.retail.inventorycount.repository.InventoryCountRepository;
import com.retail.inventorycount.service.InventoryCountService;
import com.retail.procurement.BranchInventory;
import com.retail.procurement.BranchInventoryRepository;
import com.retail.procurement.InventoryTransactionType;
import com.retail.procurement.Product;
import com.retail.disposal.service.StockDisposalService;
import com.retail.disposal.entity.DisposalSourceType;
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
        InventoryCount count = new InventoryCount();
        String code = "STK-" + request.getBranchId() + "-"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        count.setCountCode(code);
        count.setBranch(entityManager.getReference(Branch.class, request.getBranchId()));
        count.setStatus(InventoryCountStatus.Draft);
        count.setCreatedBy(entityManager.getReference(Employee.class, createdByEmployeeId));

        for (InventoryCountRequest.InventoryCountDetailDto detailDto : request.getDetails()) {
            InventoryCountDetail detail = new InventoryCountDetail();
            detail.setProduct(entityManager.getReference(Product.class, detailDto.getProductId()));

            // Dùng method findByBranchBranchIdAndProductProductId của Toàn
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
    public List<InventoryCount> getAllCounts() {
        return countRepository.findAll();
    }

    @Override
    public InventoryCount getCountById(Long countId) {
        return countRepository.findById(countId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu kiểm kê ID: " + countId));
    }
}

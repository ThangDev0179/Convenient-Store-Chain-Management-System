package com.retail.service.impl;
import com.retail.service.AuditLogService;
import com.retail.entity.Branch;
import com.retail.entity.BranchInventory;
import com.retail.entity.DisposalSourceType;
import com.retail.entity.Employee;
import com.retail.service.InventoryTransactionService;
import com.retail.entity.InventoryTransactionType;
import com.retail.entity.Product;
import com.retail.entity.StockDisposal;
import com.retail.entity.StockDisposalDetail;
import com.retail.repository.StockDisposalRepository;
import com.retail.dto.StockDisposalRequest;
import com.retail.service.StockDisposalService;
import com.retail.entity.StockDisposalStatus;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockDisposalServiceImpl implements StockDisposalService {

    private final StockDisposalRepository disposalRepository;
    private final InventoryTransactionService transactionService;
    private final AuditLogService auditLogService;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public StockDisposal createManualDisposal(StockDisposalRequest request, Long createdByEmployeeId) {
        Branch branch = entityManager.find(Branch.class, request.getBranchId());
        if (branch == null) {
            throw new IllegalArgumentException("Không tìm thấy chi nhánh ID: " + request.getBranchId());
        }
        
        StockDisposal disposal = new StockDisposal();
        String code = "DSP-" + request.getBranchId() + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        disposal.setDisposalCode(code);
        disposal.setBranch(branch);
        disposal.setStatus(StockDisposalStatus.Draft);
        disposal.setSourceType(DisposalSourceType.Manual);
        disposal.setReason(request.getReason());
        disposal.setCreatedBy(entityManager.getReference(Employee.class, createdByEmployeeId));

        for (StockDisposalRequest.DisposalDetailDto dto : request.getDetails()) {
            StockDisposalDetail detail = new StockDisposalDetail();
            Product product = entityManager.find(Product.class, dto.getProductId());
            if (product == null) {
                throw new IllegalArgumentException("Không tìm thấy sản phẩm ID: " + dto.getProductId());
            }
            detail.setProduct(product);
            detail.setQuantityDisposed(dto.getQuantityDisposed());
            detail.setUnitCost(product.getStandardPrice() != null ? product.getStandardPrice() : BigDecimal.ZERO);
            detail.setNote(dto.getNote());
            disposal.addDetail(detail);
        }

        StockDisposal saved = disposalRepository.save(disposal);
        auditLogService.logAction(createdByEmployeeId, "CreateDisposal", "StockDisposal",
                saved.getDisposalId(), null, saved.getStatus().name(), "Tạo phiếu hủy thủ công", null, null);
        return saved;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public StockDisposal autoCreateFromLoss(Integer branchId, Long productId, BigDecimal lossQty,
                                            DisposalSourceType type, Long refId, String reason, Long employeeId) {
        StockDisposal disposal = new StockDisposal();
        String prefix = type == DisposalSourceType.TransferLoss ? "DSPL-" : "DSPC-";
        String code = prefix + branchId + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        disposal.setDisposalCode(code);
        disposal.setBranch(entityManager.getReference(Branch.class, branchId));
        disposal.setStatus(StockDisposalStatus.Draft);
        disposal.setSourceType(type);
        disposal.setReferenceId(refId);
        disposal.setReason(reason);
        disposal.setCreatedBy(entityManager.getReference(Employee.class, employeeId));

        StockDisposalDetail detail = new StockDisposalDetail();
        Product product = entityManager.find(Product.class, productId);
        detail.setProduct(product);
        detail.setQuantityDisposed(lossQty);
        detail.setUnitCost(product != null && product.getStandardPrice() != null ? product.getStandardPrice() : BigDecimal.ZERO);
        detail.setNote("Tạo tự động từ hệ thống");
        disposal.addDetail(detail);

        StockDisposal saved = disposalRepository.save(disposal);
        auditLogService.logAction(employeeId, "AutoCreateDisposal", "StockDisposal",
                saved.getDisposalId(), null, saved.getStatus().name(), "Hệ thống tự động sinh phiếu hủy", null, null);
        return saved;
    }

    @Override
    @Transactional
    public StockDisposal approveDisposal(Long disposalId, Long approvedByEmployeeId) {
        StockDisposal disposal = getDisposalById(disposalId);
        if (disposal.getStatus() != StockDisposalStatus.Draft) {
            throw new IllegalStateException("Chỉ phiếu ở trạng thái Draft mới được duyệt");
        }

        String oldStatus = disposal.getStatus().name();
        disposal.setStatus(StockDisposalStatus.Completed);
        disposal.setApprovedBy(entityManager.getReference(Employee.class, approvedByEmployeeId));
        disposal.setApprovedAt(LocalDateTime.now());

        // Nếu là phiếu hủy thủ công (Manual), ta cần trừ tồn kho QtyOnHand và QtyAvailable
        // Nếu là TransferLoss hoặc CountVariance, hệ thống ĐÃ TRỪ vật lý rồi (thông qua Count/Transfer logic), 
        // phiếu Disposal này chỉ đóng vai trò ghi nhận chi phí/Báo cáo hao hụt, không trừ kho lại nữa để tránh trừ 2 lần!
        if (disposal.getSourceType() == DisposalSourceType.Manual) {
            for (StockDisposalDetail detail : disposal.getDetails()) {
                BranchInventory inventory = entityManager.createQuery("SELECT b FROM BranchInventory b WHERE b.branch.branchId = :branchId AND b.product.productId = :productId", BranchInventory.class)
                        .setParameter("branchId", disposal.getBranch().getBranchId())
                        .setParameter("productId", detail.getProduct().getProductId())
                        .getResultStream().findFirst().orElse(null);

                if (inventory == null || inventory.getQtyAvailable().compareTo(detail.getQuantityDisposed()) < 0) {
                    throw new IllegalStateException("Sản phẩm ID " + detail.getProduct().getProductId() + " không đủ số lượng tồn khả dụng để xuất hủy.");
                }

                transactionService.recordTransaction(
                        disposal.getBranch().getBranchId(),
                        detail.getProduct().getProductId(),
                        detail.getQuantityDisposed().negate(), // Trừ QtyOnHand
                        detail.getQuantityDisposed().negate(), // Trừ QtyAvailable
                        BigDecimal.ZERO,
                        InventoryTransactionType.Disposal,
                        "StockDisposal", disposalId,
                        "Duyệt xuất hủy thủ công: " + disposal.getDisposalCode(),
                        approvedByEmployeeId
                );
            }
        }

        StockDisposal saved = disposalRepository.save(disposal);
        auditLogService.logAction(approvedByEmployeeId, "ApproveDisposal", "StockDisposal",
                saved.getDisposalId(), oldStatus, saved.getStatus().name(), "Duyệt phiếu xuất hủy", null, null);
        return saved;
    }

    @Override
    @Transactional
    public StockDisposal rejectDisposal(Long disposalId, Long rejectedByEmployeeId) {
        StockDisposal disposal = getDisposalById(disposalId);
        if (disposal.getStatus() != StockDisposalStatus.Draft) {
            throw new IllegalStateException("Chỉ phiếu ở trạng thái Draft mới được từ chối");
        }

        String oldStatus = disposal.getStatus().name();
        disposal.setStatus(StockDisposalStatus.Canceled);
        StockDisposal saved = disposalRepository.save(disposal);
        auditLogService.logAction(rejectedByEmployeeId, "RejectDisposal", "StockDisposal",
                saved.getDisposalId(), oldStatus, saved.getStatus().name(), "Từ chối phiếu xuất hủy", null, null);
        return saved;
    }

    @Override
    public List<StockDisposal> getAllDisposals() {
        return disposalRepository.findAll();
    }

    @Override
    public StockDisposal getDisposalById(Long disposalId) {
        return disposalRepository.findById(disposalId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu xuất hủy ID: " + disposalId));
    }
}
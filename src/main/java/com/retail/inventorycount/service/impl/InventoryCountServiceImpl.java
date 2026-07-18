package com.retail.inventorycount.service.impl;

import com.retail.audit.service.AuditLogService;
import com.retail.branch.Branch;
import com.retail.employee.Employee;
import com.retail.inventory.entity.BranchInventory;
import com.retail.inventory.entity.BranchInventoryId;
import com.retail.inventory.entity.TransactionType;
import com.retail.inventory.repository.BranchInventoryRepository;
import com.retail.inventory.service.InventoryTransactionService;
import com.retail.inventorycount.dto.InventoryCountRequest;
import com.retail.inventorycount.entity.InventoryCount;
import com.retail.inventorycount.entity.InventoryCountDetail;
import com.retail.inventorycount.entity.InventoryCountStatus;
import com.retail.inventorycount.repository.InventoryCountRepository;
import com.retail.inventorycount.service.InventoryCountService;
import com.retail.product.Product;
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
    private final AuditLogService auditLogService;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public InventoryCount createDraftCount(InventoryCountRequest request, Long createdByEmployeeId) {
        InventoryCount count = new InventoryCount();
        String code = "STK-" + request.getBranchId() + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        count.setCountCode(code);
        count.setBranch(entityManager.getReference(Branch.class, request.getBranchId()));
        count.setStatus(InventoryCountStatus.Draft);
        count.setCreatedBy(entityManager.getReference(Employee.class, createdByEmployeeId));

        for (InventoryCountRequest.InventoryCountDetailDto detailDto : request.getDetails()) {
            InventoryCountDetail detail = new InventoryCountDetail();
            detail.setProduct(entityManager.getReference(Product.class, detailDto.getProductId()));
            
            BranchInventoryId invId = new BranchInventoryId(request.getBranchId(), detailDto.getProductId());
            BigDecimal systemQty = branchInventoryRepository.findById(invId)
                    .map(BranchInventory::getQtyOnHand)
                    .orElse(BigDecimal.ZERO);
                    
            detail.setSystemQty(systemQty);
            detail.setActualQty(detailDto.getActualQty());
            
            count.addDetail(detail);
        }

        InventoryCount saved = countRepository.save(count);
        auditLogService.logAction(createdByEmployeeId, "CreateInventoryCount", "InventoryCount", saved.getInventoryCountId(), null, saved.getStatus().name(), "Created Draft", null, null);
        return saved;
    }

    @Override
    @Transactional
    public InventoryCount submitCount(Long countId, Long employeeId) {
        InventoryCount count = getCountById(countId);
        if (count.getStatus() != InventoryCountStatus.Draft) {
            throw new IllegalStateException("Only Draft count can be submitted");
        }
        String oldStatus = count.getStatus().name();
        count.setStatus(InventoryCountStatus.Submitted);
        count.setSubmittedAt(LocalDateTime.now());
        
        InventoryCount saved = countRepository.save(count);
        auditLogService.logAction(employeeId, "SubmitInventoryCount", "InventoryCount", saved.getInventoryCountId(), oldStatus, saved.getStatus().name(), "Submitted for approval", null, null);
        return saved;
    }

    @Override
    @Transactional
    public InventoryCount approveCount(Long countId, Long approvedByEmployeeId) {
        InventoryCount count = getCountById(countId);
        if (count.getStatus() != InventoryCountStatus.Submitted) {
            throw new IllegalStateException("Only Submitted count can be approved");
        }
        
        String oldStatus = count.getStatus().name();
        count.setStatus(InventoryCountStatus.Approved);
        count.setApprovedBy(entityManager.getReference(Employee.class, approvedByEmployeeId));
        count.setApprovedAt(LocalDateTime.now());

        for (InventoryCountDetail detail : count.getDetails()) {
            BigDecimal actual = detail.getActualQty();
            BigDecimal system = detail.getSystemQty();
            BigDecimal delta = actual.subtract(system);

            if (delta.compareTo(BigDecimal.ZERO) != 0) {
                transactionService.recordTransaction(
                        count.getBranch().getBranchId(),
                        detail.getProduct().getProductId(),
                        delta, 
                        delta, 
                        BigDecimal.ZERO, 
                        TransactionType.CountAdjustment,
                        "InventoryCount",
                        count.getInventoryCountId(),
                        "Inventory Count Approved: " + count.getCountCode(),
                        approvedByEmployeeId
                );
            }
        }

        InventoryCount saved = countRepository.save(count);
        auditLogService.logAction(approvedByEmployeeId, "ApproveInventoryCount", "InventoryCount", saved.getInventoryCountId(), oldStatus, saved.getStatus().name(), "Approved and inventory updated", null, null);
        return saved;
    }

    @Override
    @Transactional
    public InventoryCount rejectCount(Long countId, Long rejectedByEmployeeId) {
        InventoryCount count = getCountById(countId);
        if (count.getStatus() != InventoryCountStatus.Submitted) {
            throw new IllegalStateException("Only Submitted count can be rejected");
        }
        
        String oldStatus = count.getStatus().name();
        count.setStatus(InventoryCountStatus.Rejected);
        
        InventoryCount saved = countRepository.save(count);
        auditLogService.logAction(rejectedByEmployeeId, "RejectInventoryCount", "InventoryCount", saved.getInventoryCountId(), oldStatus, saved.getStatus().name(), "Rejected", null, null);
        return saved;
    }

    @Override
    public List<InventoryCount> getAllCounts() {
        return countRepository.findAll();
    }

    @Override
    public InventoryCount getCountById(Long countId) {
        return countRepository.findById(countId)
                .orElseThrow(() -> new IllegalArgumentException("Inventory count not found with id: " + countId));
    }
}

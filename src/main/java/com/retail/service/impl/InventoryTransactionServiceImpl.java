package com.retail.service.impl;
import com.retail.entity.Branch;
import com.retail.entity.BranchInventory;
import com.retail.repository.BranchInventoryRepository;
import com.retail.entity.Employee;
import com.retail.entity.InventoryTransactionHistory;
import com.retail.repository.InventoryTransactionHistoryRepository;
import com.retail.service.InventoryTransactionService;
import com.retail.entity.InventoryTransactionType;
import com.retail.entity.Product;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class InventoryTransactionServiceImpl implements InventoryTransactionService {

    private final BranchInventoryRepository branchInventoryRepository;
    private final InventoryTransactionHistoryRepository transactionHistoryRepository;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public void recordTransaction(
            Integer branchId,
            Long productId,
            BigDecimal qtyOnHandChange,
            BigDecimal qtyAvailableChange,
            BigDecimal qtyInTransitChange,
            InventoryTransactionType transactionType,
            String referenceTable,
            Long referenceId,
            String reason,
            Long createdById) {

        // Dùng method của Toàn: findByBranchBranchIdAndProductProductId
        BranchInventory inventory = branchInventoryRepository
                .findByBranchBranchIdAndProductProductId(branchId, productId)
                .orElseGet(() -> {
                    BranchInventory newInv = new BranchInventory();
                    newInv.setBranch(entityManager.getReference(Branch.class, branchId));
                    newInv.setProduct(entityManager.getReference(Product.class, productId));
                    newInv.setQtyOnHand(BigDecimal.ZERO);
                    newInv.setQtyAvailable(BigDecimal.ZERO);
                    newInv.setQtyInTransit(BigDecimal.ZERO);
                    return newInv;
                });

        inventory.setQtyOnHand(inventory.getQtyOnHand().add(qtyOnHandChange));
        inventory.setQtyAvailable(inventory.getQtyAvailable().add(qtyAvailableChange));
        inventory.setQtyInTransit(inventory.getQtyInTransit().add(qtyInTransitChange));

        if (inventory.getQtyOnHand().compareTo(BigDecimal.ZERO) < 0 ||
            inventory.getQtyAvailable().compareTo(BigDecimal.ZERO) < 0 ||
            inventory.getQtyInTransit().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException(
                "Số lượng tồn kho không thể âm — sản phẩm ID=" + productId + " tại chi nhánh ID=" + branchId);
        }

        branchInventoryRepository.save(inventory);

        InventoryTransactionHistory history = new InventoryTransactionHistory();
        history.setBranch(entityManager.getReference(Branch.class, branchId));
        history.setProduct(entityManager.getReference(Product.class, productId));
        history.setTransactionType(transactionType);
        history.setReferenceTable(referenceTable);
        history.setReferenceId(referenceId);
        history.setQuantityChange(qtyOnHandChange);
        history.setReason(reason);
        if (createdById != null) {
            history.setCreatedBy(entityManager.getReference(Employee.class, createdById));
        }

        transactionHistoryRepository.save(history);
    }
}
package com.retail.inventory.service.impl;

import com.retail.branch.Branch;
import com.retail.employee.Employee;
import com.retail.inventory.entity.BranchInventory;
import com.retail.inventory.entity.BranchInventoryId;
import com.retail.inventory.entity.InventoryTransactionHistory;
import com.retail.inventory.entity.TransactionType;
import com.retail.inventory.repository.BranchInventoryRepository;
import com.retail.inventory.repository.InventoryTransactionHistoryRepository;
import com.retail.inventory.service.InventoryTransactionService;
import com.retail.product.Product;
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
            TransactionType transactionType,
            String referenceTable,
            Long referenceId,
            String reason,
            Long createdById) {

        BranchInventoryId inventoryId = new BranchInventoryId(branchId, productId);
        BranchInventory inventory = branchInventoryRepository.findById(inventoryId)
                .orElseGet(() -> {
                    BranchInventory newInv = new BranchInventory();
                    newInv.setId(inventoryId);
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
            throw new IllegalStateException("Inventory quantity cannot be negative for product " + productId + " at branch " + branchId);
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

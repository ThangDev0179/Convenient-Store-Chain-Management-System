package com.retail.inventory.repository;

import com.retail.inventory.entity.InventoryTransactionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryTransactionHistoryRepository extends JpaRepository<InventoryTransactionHistory, Long> {
    List<InventoryTransactionHistory> findByBranchBranchIdAndProductProductId(Integer branchId, Long productId);
    List<InventoryTransactionHistory> findByReferenceTableAndReferenceId(String referenceTable, Long referenceId);
}

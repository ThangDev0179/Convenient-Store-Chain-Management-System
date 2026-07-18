package com.retail.inventory.repository;

import com.retail.inventory.entity.BranchInventory;
import com.retail.inventory.entity.BranchInventoryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BranchInventoryRepository extends JpaRepository<BranchInventory, BranchInventoryId> {
    List<BranchInventory> findByIdBranchId(Integer branchId);
    List<BranchInventory> findByIdProductId(Long productId);
}

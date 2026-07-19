package com.retail.repository;
import com.retail.entity.BranchInventory;
import com.retail.entity.BranchInventoryId;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BranchInventoryRepository extends JpaRepository<BranchInventory, BranchInventoryId> {
    Optional<BranchInventory> findByBranchBranchIdAndProductProductId(Integer branchId, Long productId);

    @Query("SELECT bi FROM BranchInventory bi WHERE " +
           "(:branchId IS NULL OR bi.branch.branchId = :branchId) " +
           "AND (:keyword IS NULL OR LOWER(bi.product.productName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(bi.product.sku) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:categoryId IS NULL OR bi.product.category.categoryId = :categoryId)")
    Page<BranchInventory> searchInventory(@Param("branchId") Integer branchId,
                                          @Param("keyword") String keyword,
                                          @Param("categoryId") Integer categoryId,
                                          Pageable pageable);

    java.util.List<BranchInventory> findByBranchBranchId(Integer branchId);
}
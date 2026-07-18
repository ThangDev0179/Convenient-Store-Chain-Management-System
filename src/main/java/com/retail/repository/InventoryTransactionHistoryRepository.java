package com.retail.repository;

import com.retail.entity.InventoryTransactionHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface InventoryTransactionHistoryRepository extends JpaRepository<InventoryTransactionHistory, Long> {
    
    @Query("SELECT ith FROM InventoryTransactionHistory ith WHERE " +
           "(:branchId IS NULL OR ith.branch.branchId = :branchId) " +
           "AND (:productId IS NULL OR ith.product.productId = :productId) " +
           "AND (:startDate IS NULL OR ith.createdAt >= :startDate) " +
           "AND (:endDate IS NULL OR ith.createdAt <= :endDate) " +
           "AND (:transactionType IS NULL OR ith.transactionType = :transactionType)")
    Page<InventoryTransactionHistory> filterHistory(@Param("branchId") Integer branchId,
                                                    @Param("productId") Long productId,
                                                    @Param("startDate") LocalDateTime startDate,
                                                    @Param("endDate") LocalDateTime endDate,
                                                    @Param("transactionType") String transactionType,
                                                    Pageable pageable);
}

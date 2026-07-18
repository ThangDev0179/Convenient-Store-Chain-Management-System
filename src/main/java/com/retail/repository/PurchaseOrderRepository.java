package com.retail.repository;

import com.retail.entity.PurchaseOrder;
import com.retail.entity.PurchaseOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    Optional<PurchaseOrder> findByPoCode(String poCode);

    @Query("SELECT MAX(po.poCode) FROM PurchaseOrder po WHERE po.branch.branchCode = :branchCode AND po.poCode LIKE CONCAT('PO-', :branchCode, '-', :dateStr, '-%')")
    String findMaxPoCodeByBranchAndDate(@Param("branchCode") String branchCode, @Param("dateStr") String dateStr);

    @Query("SELECT po FROM PurchaseOrder po WHERE " +
           "(:branchId IS NULL OR po.branch.branchId = :branchId) " +
           "AND (:status IS NULL OR po.status = :status) " +
           "AND (:keyword IS NULL OR LOWER(po.poCode) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(po.supplier.supplierName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<PurchaseOrder> searchPurchaseOrders(@Param("branchId") Integer branchId,
                                             @Param("status") PurchaseOrderStatus status,
                                             @Param("keyword") String keyword,
                                             Pageable pageable);
}

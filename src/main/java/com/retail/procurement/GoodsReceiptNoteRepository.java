package com.retail.procurement;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GoodsReceiptNoteRepository extends JpaRepository<GoodsReceiptNote, Long> {

    @Query("SELECT MAX(g.grnCode) FROM GoodsReceiptNote g WHERE g.branch.branchCode = :branchCode AND g.grnCode LIKE CONCAT('GRN-', :branchCode, '-', :dateStr, '-%')")
    String findMaxGrnCodeByBranchAndDate(@Param("branchCode") String branchCode, @Param("dateStr") String dateStr);

    java.util.List<GoodsReceiptNote> findByPurchaseOrderPurchaseOrderIdAndStatus(Long poId, String status);
}

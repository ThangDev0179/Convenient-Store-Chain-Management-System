package com.retail.repository;
import com.retail.entity.StockTransfer;
import com.retail.entity.StockTransferStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockTransferRepository extends JpaRepository<StockTransfer, Long> {
    List<StockTransfer> findByFromBranchBranchId(Integer fromBranchId);
    List<StockTransfer> findByToBranchBranchId(Integer toBranchId);
    List<StockTransfer> findByStatus(StockTransferStatus status);
    boolean existsByTransferCode(String transferCode);
    List<StockTransfer> findByFromBranchBranchIdAndCreatedAtAfter(Integer branchId, java.time.LocalDateTime startOfDay);
    
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(st) FROM StockTransfer st WHERE st.status = com.retail.entity.StockTransferStatus.In_Transit AND (st.fromBranch.branchId = :branchId OR st.toBranch.branchId = :branchId)")
    long countActiveTransfers(@org.springframework.data.repository.query.Param("branchId") Integer branchId);
}
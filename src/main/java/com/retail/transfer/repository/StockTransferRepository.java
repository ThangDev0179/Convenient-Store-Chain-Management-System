package com.retail.transfer.repository;

import com.retail.transfer.entity.StockTransfer;
import com.retail.transfer.entity.StockTransferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockTransferRepository extends JpaRepository<StockTransfer, Long> {
    List<StockTransfer> findByFromBranchBranchId(Integer fromBranchId);
    List<StockTransfer> findByToBranchBranchId(Integer toBranchId);
    List<StockTransfer> findByStatus(StockTransferStatus status);
    boolean existsByTransferCode(String transferCode);
}

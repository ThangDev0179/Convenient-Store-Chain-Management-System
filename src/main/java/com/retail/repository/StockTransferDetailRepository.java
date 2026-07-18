package com.retail.repository;
import com.retail.entity.StockTransferDetail;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockTransferDetailRepository extends JpaRepository<StockTransferDetail, Long> {
    List<StockTransferDetail> findByStockTransferStockTransferId(Long stockTransferId);
}
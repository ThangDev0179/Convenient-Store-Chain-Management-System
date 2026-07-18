package com.retail.repository;
import com.retail.entity.StockDisposalDetail;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockDisposalDetailRepository extends JpaRepository<StockDisposalDetail, Long> {
    List<StockDisposalDetail> findByStockDisposalDisposalId(Long disposalId);
}
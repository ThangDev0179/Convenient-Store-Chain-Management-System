package com.retail.disposal.repository;

import com.retail.disposal.entity.StockDisposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockDisposalRepository extends JpaRepository<StockDisposal, Long> {
}

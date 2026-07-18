package com.retail.repository;
import com.retail.entity.StockDisposal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockDisposalRepository extends JpaRepository<StockDisposal, Long> {
}
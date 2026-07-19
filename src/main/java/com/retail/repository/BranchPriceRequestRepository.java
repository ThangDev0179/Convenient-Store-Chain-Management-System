package com.retail.repository;

import com.retail.entity.BranchPriceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BranchPriceRequestRepository extends JpaRepository<BranchPriceRequest, Long> {
    List<BranchPriceRequest> findByBranchBranchId(Integer branchId);
}

package com.retail.repository;

import com.retail.entity.BranchProductPrice;
import com.retail.entity.BranchProductPriceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface BranchProductPriceRepository extends JpaRepository<BranchProductPrice, BranchProductPriceId> {
    Optional<BranchProductPrice> findByBranchBranchIdAndProductProductId(Integer branchId, Long productId);
}

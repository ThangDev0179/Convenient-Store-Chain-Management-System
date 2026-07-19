package com.retail.repository;

import com.retail.common.stub.BranchProductPriceStub;
import com.retail.common.stub.BranchProductPriceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BranchProductPriceStubRepository extends JpaRepository<BranchProductPriceStub, BranchProductPriceId> {
}

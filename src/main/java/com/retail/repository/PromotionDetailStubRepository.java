package com.retail.repository;

import com.retail.common.stub.PromotionDetailStub;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PromotionDetailStubRepository extends JpaRepository<PromotionDetailStub, Long> {
}

package com.retail.repository;

import com.retail.entity.PromotionDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PromotionDetailRepository extends JpaRepository<PromotionDetail, Long> {
    List<PromotionDetail> findByPromotionPromotionId(Long promotionId);
    boolean existsByPromotionPromotionIdAndProductProductId(Long promotionId, Long productId);
}

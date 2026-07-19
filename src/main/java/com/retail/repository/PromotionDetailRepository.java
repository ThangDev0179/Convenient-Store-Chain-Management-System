package com.retail.repository;

import com.retail.entity.PromotionDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PromotionDetailRepository extends JpaRepository<PromotionDetail, Long> {
    @Query("SELECT pd FROM PromotionDetail pd WHERE pd.product.productId = :productId " +
           "AND pd.promotion.status = 'Active' " +
           "AND pd.promotion.startDateTime <= :now " +
           "AND pd.promotion.endDateTime >= :now")
    List<PromotionDetail> findActivePromotionsForProduct(@Param("productId") Long productId, @Param("now") LocalDateTime now);
}

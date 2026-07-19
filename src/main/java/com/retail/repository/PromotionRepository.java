package com.retail.repository;

import com.retail.entity.Promotion;
import com.retail.entity.PromotionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    @Query("SELECT p FROM Promotion p WHERE " +
           "(:keyword IS NULL OR LOWER(p.promotionName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:status IS NULL OR p.status = :status) " +
           "ORDER BY p.createdAt DESC")
    Page<Promotion> search(@Param("keyword") String keyword,
                           @Param("status") PromotionStatus status,
                           Pageable pageable);

    boolean existsByPromotionName(String promotionName);
    boolean existsByPromotionNameAndPromotionIdNot(String promotionName, Long promotionId);
}

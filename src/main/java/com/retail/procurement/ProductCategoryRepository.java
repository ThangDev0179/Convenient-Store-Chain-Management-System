package com.retail.procurement;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Integer> {
    Optional<ProductCategory> findBySkuPrefix(String skuPrefix);

    @Query("SELECT c FROM ProductCategory c WHERE " +
           "(:keyword IS NULL OR LOWER(c.categoryName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(c.skuPrefix) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<ProductCategory> searchCategories(@Param("keyword") String keyword, Pageable pageable);
}

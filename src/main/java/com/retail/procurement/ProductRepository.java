package com.retail.procurement;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findBySku(String sku);

    List<Product> findByStatus(ProductStatus status);

    @Query("SELECT MAX(p.sku) FROM Product p WHERE p.sku LIKE CONCAT(:prefix, '-%')")
    String findMaxSkuByPrefix(@Param("prefix") String prefix);

    @Query("SELECT p FROM Product p WHERE " +
           "(:keyword IS NULL OR LOWER(p.productName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:categoryId IS NULL OR p.category.categoryId = :categoryId) " +
           "AND (:supplierId IS NULL OR p.defaultSupplier.supplierId = :supplierId) " +
           "AND (:status IS NULL OR p.status = :status)")
    Page<Product> searchProducts(@Param("keyword") String keyword,
                                 @Param("categoryId") Integer categoryId,
                                 @Param("supplierId") Integer supplierId,
                                 @Param("status") ProductStatus status,
                                 Pageable pageable);
}

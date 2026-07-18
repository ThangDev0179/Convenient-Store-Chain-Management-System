package com.retail.procurement;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductUOMRepository extends JpaRepository<ProductUOM, Long> {
    List<ProductUOM> findByProductProductId(Long productId);
    Optional<ProductUOM> findByProductProductIdAndUomName(Long productId, String uomName);
}

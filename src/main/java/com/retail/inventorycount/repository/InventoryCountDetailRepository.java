package com.retail.inventorycount.repository;

import com.retail.inventorycount.entity.InventoryCountDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryCountDetailRepository extends JpaRepository<InventoryCountDetail, Long> {
    List<InventoryCountDetail> findByInventoryCountInventoryCountId(Long inventoryCountId);
}

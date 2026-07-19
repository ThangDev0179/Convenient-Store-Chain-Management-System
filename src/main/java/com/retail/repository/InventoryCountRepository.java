package com.retail.repository;
import com.retail.entity.InventoryCount;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryCountRepository extends JpaRepository<InventoryCount, Long> {
    Optional<InventoryCount> findByCountCode(String countCode);
    List<InventoryCount> findByBranchBranchId(Integer branchId);
    List<InventoryCount> findByBranchBranchIdAndCreatedAtAfter(Integer branchId, java.time.LocalDateTime startOfDay);
}
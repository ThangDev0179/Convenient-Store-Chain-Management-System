package com.retail.repository;

import com.retail.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {
    Optional<Refund> findByRefundCode(String refundCode);

    @Query("SELECT MAX(r.refundCode) FROM Refund r WHERE r.branch.branchCode = :branchCode AND r.refundCode LIKE CONCAT('REF-', :branchCode, '-', :dateStr, '-%')")
    String findMaxRefundCodeByBranchAndDate(@Param("branchCode") String branchCode, @Param("dateStr") String dateStr);
}

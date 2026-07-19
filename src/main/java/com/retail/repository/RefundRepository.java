package com.retail.repository;

import com.retail.entity.Refund;
import com.retail.entity.RefundStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long>,
        JpaSpecificationExecutor<Refund> {

    boolean existsByRefundCode(String refundCode);

    /**
     * Count refunds for a branch today — used for sequential RefundCode generation.
     * Format: REF-[BranchCode]-YYYYMMDD-[4-digit sequence].
     */
    @Query("SELECT COUNT(r) FROM Refund r WHERE r.branchId = :branchId " +
           "AND r.createdAt >= :startOfDay AND r.createdAt < :endOfDay")
    long countByBranchIdAndCreatedAtBetween(@Param("branchId") Integer branchId,
                                            @Param("startOfDay") LocalDateTime startOfDay,
                                            @Param("endOfDay") LocalDateTime endOfDay);

    /** Eager fetch with details to avoid N+1 on detail view */
    @Query("SELECT DISTINCT r FROM Refund r LEFT JOIN FETCH r.details WHERE r.refundId = :id")
    Optional<Refund> findByIdWithDetails(@Param("id") Long id);

    Page<Refund> findByStatus(RefundStatus status, Pageable pageable);
}

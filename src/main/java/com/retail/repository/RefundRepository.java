package com.retail.repository;

import com.retail.entity.Refund;
import com.retail.entity.RefundStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository for Refund entity.
 *
 * NOTE (HSF302 compliance — Mục 3):
 *   Đã bỏ JpaSpecificationExecutor và Specification API.
 *   Thay bằng @Query JPQL với nullable params (pattern đã học ở Chapter04).
 */
@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {

    boolean existsByRefundCode(String refundCode);

    /**
     * Count refunds for a branch today — sinh RefundCode tuần tự.
     * Format: REF-[BranchCode]-YYYYMMDD-[4 số].
     */
    @Query("SELECT COUNT(r) FROM Refund r WHERE r.branchId = :branchId " +
           "AND r.createdAt >= :startOfDay AND r.createdAt < :endOfDay")
    long countByBranchIdAndCreatedAtBetween(@Param("branchId") Integer branchId,
                                            @Param("startOfDay") LocalDateTime startOfDay,
                                            @Param("endOfDay") LocalDateTime endOfDay);

    /**
     * Phân trang danh sách refund với filter động (thay thế Specification API).
     *
     * Tham số nullable: null → bỏ qua filter đó.
     */
    @Query("SELECT r FROM Refund r WHERE " +
           "(:status IS NULL OR r.status = :status) AND " +
           "(:fromDate IS NULL OR r.createdAt >= :fromDate) AND " +
           "(:toDate IS NULL OR r.createdAt < :toDate) AND " +
           "(:branchId IS NULL OR r.branchId = :branchId)")
    Page<Refund> findByFilters(
            @Param("status") RefundStatus status,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("branchId") Integer branchId,
            Pageable pageable);

    /** Eager fetch với details tránh N+1 khi hiển thị chi tiết refund */
    @Query("SELECT DISTINCT r FROM Refund r LEFT JOIN FETCH r.details WHERE r.refundId = :id")
    Optional<Refund> findByIdWithDetails(@Param("id") Long id);
}

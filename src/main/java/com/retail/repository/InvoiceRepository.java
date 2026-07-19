package com.retail.repository;

import com.retail.entity.Invoice;
import com.retail.entity.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository for Invoice entity.
 *
 * NOTE (HSF302 compliance — Mục 3):
 *   Đã bỏ JpaSpecificationExecutor và Specification API (chỉ nêu tên trong slide,
 *   không có bài demo). Thay bằng @Query JPQL với tham số nullable để lọc động —
 *   đây là kỹ thuật được dạy trong Chapter04.
 *
 *   Pattern dùng: WHERE (:param IS NULL OR field = :param)
 *   → nếu caller truyền null thì điều kiện luôn đúng (bỏ qua filter đó).
 */
@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByInvoiceCode(String invoiceCode);

    boolean existsByInvoiceCode(String invoiceCode);

    /**
     * Count invoices created today for a branch — dùng sinh InvoiceCode tuần tự.
     * Format: INV-[BranchCode]-YYYYMMDD-[6 số].
     */
    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.branchId = :branchId " +
           "AND i.createdAt >= :startOfDay AND i.createdAt < :endOfDay")
    long countByBranchIdAndCreatedAtBetween(@Param("branchId") Integer branchId,
                                            @Param("startOfDay") LocalDateTime startOfDay,
                                            @Param("endOfDay") LocalDateTime endOfDay);

    /**
     * Phân trang danh sách hóa đơn với filter động (thay thế Specification API).
     *
     * Các tham số đều nullable:
     *   - status    : null → không lọc theo trạng thái
     *   - fromDate  : null → không chặn dưới theo ngày
     *   - toDate    : null → không chặn trên theo ngày
     *   - cashierId : null → không lọc theo thu ngân
     *   - branchId  : null → không lọc theo chi nhánh (ADMIN/MANAGER thấy tất cả)
     */
    @Query("SELECT i FROM Invoice i WHERE " +
           "i.status IN :statuses AND " +
           "(:fromDate IS NULL OR i.createdAt >= :fromDate) AND " +
           "(:toDate IS NULL OR i.createdAt < :toDate) AND " +
           "(:cashierId IS NULL OR i.cashierId = :cashierId) AND " +
           "(:branchId IS NULL OR i.branchId = :branchId)")
    Page<Invoice> findByFilters(
            @Param("statuses") java.util.List<InvoiceStatus> statuses,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("cashierId") Long cashierId,
            @Param("branchId") Integer branchId,
            Pageable pageable);

    /**
     * Fetch invoice kèm details (tránh N+1 khi hiển thị chi tiết hóa đơn).
     */
    @Query("SELECT DISTINCT i FROM Invoice i LEFT JOIN FETCH i.details WHERE i.invoiceId = :id")
    Optional<Invoice> findByIdWithDetails(@Param("id") Long id);
}

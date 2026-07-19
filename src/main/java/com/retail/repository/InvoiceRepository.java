package com.retail.repository;

import com.retail.entity.Invoice;
import com.retail.entity.InvoiceStatus;
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
public interface InvoiceRepository extends JpaRepository<Invoice, Long>,
        JpaSpecificationExecutor<Invoice> {

    Optional<Invoice> findByInvoiceCode(String invoiceCode);

    boolean existsByInvoiceCode(String invoiceCode);

    /**
     * Count invoices created today for a branch — used for sequential InvoiceCode generation.
     * Format: INV-[BranchCode]-YYYYMMDD-[6-digit sequence].
     */
    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.branchId = :branchId " +
           "AND i.createdAt >= :startOfDay AND i.createdAt < :endOfDay")
    long countByBranchIdAndCreatedAtBetween(@Param("branchId") Integer branchId,
                                            @Param("startOfDay") LocalDateTime startOfDay,
                                            @Param("endOfDay") LocalDateTime endOfDay);

    /**
     * Paginated list with filters: status, date range, cashier.
     * Uses Specification for dynamic filtering — see InvoiceSpecification.
     */
    Page<Invoice> findByBranchIdAndStatus(Integer branchId, InvoiceStatus status, Pageable pageable);

    Page<Invoice> findByStatus(InvoiceStatus status, Pageable pageable);

    /**
     * Fetch invoice with details eagerly (avoid N+1 for detail view).
     */
    @Query("SELECT DISTINCT i FROM Invoice i LEFT JOIN FETCH i.details WHERE i.invoiceId = :id")
    Optional<Invoice> findByIdWithDetails(@Param("id") Long id);
}

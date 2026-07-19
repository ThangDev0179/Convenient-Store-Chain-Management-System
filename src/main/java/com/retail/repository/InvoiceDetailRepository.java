package com.retail.repository;

import com.retail.entity.InvoiceDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceDetailRepository extends JpaRepository<InvoiceDetail, Long> {

    List<InvoiceDetail> findByInvoice_InvoiceId(Long invoiceId);

    /**
     * Rule #10: Check if a product already exists in this invoice (to merge quantities, not create duplicate row).
     */
    Optional<InvoiceDetail> findByInvoice_InvoiceIdAndProductId(Long invoiceId, Long productId);

    /**
     * Used by Refund module: find all details of an invoice for a specific product
     * to compute the original UnitPrice and cumulative quantity sold.
     */
    @Query("SELECT d FROM InvoiceDetail d WHERE d.invoice.invoiceId = :invoiceId AND d.productId = :productId")
    Optional<InvoiceDetail> findByInvoiceIdAndProductId(@Param("invoiceId") Long invoiceId,
                                                        @Param("productId") Long productId);
}

package com.retail.repository;

import com.retail.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByInvoiceCode(String invoiceCode);

    @Query("SELECT MAX(i.invoiceCode) FROM Invoice i WHERE i.branch.branchCode = :branchCode AND i.invoiceCode LIKE CONCAT('INV-', :branchCode, '-', :dateStr, '-%')")
    String findMaxInvoiceCodeByBranchAndDate(@Param("branchCode") String branchCode, @Param("dateStr") String dateStr);
}

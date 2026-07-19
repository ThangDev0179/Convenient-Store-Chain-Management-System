package com.retail.repository;

import com.retail.entity.SupplierInvoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SupplierInvoiceRepository extends JpaRepository<SupplierInvoice, Long> {

    Optional<SupplierInvoice> findByGrnGrnId(Long grnId);

    @Query("SELECT s FROM SupplierInvoice s WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           "s.grn.grnCode LIKE CONCAT('%', :keyword, '%') OR " +
           "s.supplier.supplierName LIKE CONCAT('%', :keyword, '%') OR " +
           "s.status LIKE CONCAT('%', :keyword, '%'))")
    Page<SupplierInvoice> searchInvoices(@Param("keyword") String keyword, Pageable pageable);
}

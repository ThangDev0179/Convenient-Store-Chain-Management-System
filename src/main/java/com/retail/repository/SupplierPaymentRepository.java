package com.retail.repository;

import com.retail.entity.SupplierPayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SupplierPaymentRepository extends JpaRepository<SupplierPayment, Long> {

    List<SupplierPayment> findBySupplierInvoiceSupplierInvoiceId(Long supplierInvoiceId);

    @Query("SELECT p FROM SupplierPayment p WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           "p.supplierInvoice.grn.grnCode LIKE CONCAT('%', :keyword, '%') OR " +
           "p.paymentMethod LIKE CONCAT('%', :keyword, '%') OR " +
           "p.supplierInvoice.supplier.supplierName LIKE CONCAT('%', :keyword, '%'))")
    Page<SupplierPayment> searchPayments(@Param("keyword") String keyword, Pageable pageable);
}

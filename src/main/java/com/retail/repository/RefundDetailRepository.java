package com.retail.repository;

import com.retail.entity.RefundDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface RefundDetailRepository extends JpaRepository<RefundDetail, Long> {

    List<RefundDetail> findByRefund_RefundId(Long refundId);

    /**
     * Rule #5: Total quantity already refunded (Completed + Pending_Approval) for
     * a specific product from a specific original invoice.
     * Used to check if current refund request exceeds original quantity.
     */
    @Query("SELECT COALESCE(SUM(rd.quantity), 0) FROM RefundDetail rd " +
           "WHERE rd.productId = :productId " +
           "AND rd.refund.originalInvoiceId = :originalInvoiceId " +
           "AND rd.refund.status IN ('Completed', 'Pending_Approval')")
    BigDecimal sumRefundedQuantity(@Param("productId") Long productId,
                                   @Param("originalInvoiceId") Long originalInvoiceId);
}

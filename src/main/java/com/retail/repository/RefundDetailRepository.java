package com.retail.repository;

import com.retail.entity.RefundDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;

@Repository
public interface RefundDetailRepository extends JpaRepository<RefundDetail, Long> {
    @Query("SELECT COALESCE(SUM(rd.quantity), 0) FROM RefundDetail rd WHERE rd.refund.originalInvoice.invoiceId = :invoiceId AND rd.product.productId = :productId AND rd.refund.status = 'Completed'")
    BigDecimal sumRefundedQtyForProduct(@Param("invoiceId") Long invoiceId, @Param("productId") Long productId);
}

package com.retail.validator;

import com.retail.common.exception.BusinessRuleViolationException;
import com.retail.entity.Invoice;
import com.retail.entity.InvoiceDetail;
import com.retail.entity.InvoiceStatus;
import com.retail.dto.CreateRefundRequest;
import com.retail.dto.RefundItemRequest;
import com.retail.repository.RefundDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Business rule validator for Refund creation (Section 6, Rules #4–#8).
 * Called within the same @Transactional in RefundServiceImpl.create().
 */
@Component
@RequiredArgsConstructor
public class RefundValidator {

    private static final BigDecimal REFUND_WINDOW_DAYS = BigDecimal.valueOf(7);

    private final RefundDetailRepository refundDetailRepository;

    /**
     * Validates all business rules before creating a Refund.
     *
     * @param request         the create refund request
     * @param originalInvoice the resolved original Invoice entity
     */
    public void validate(CreateRefundRequest request, Invoice originalInvoice, Integer currentEmployeeBranchId) {

        // Rule #6: Refund.BranchId must match Invoice.BranchId
        if (!currentEmployeeBranchId.equals(originalInvoice.getBranchId())) {
            throw new BusinessRuleViolationException("RULE_6",
                    "You can only create refunds for invoices from your own branch.");
        }

        // Rule #7: Cannot refund an invoice that is not Paid
        if (originalInvoice.getStatus() != InvoiceStatus.Paid) {
            throw new BusinessRuleViolationException("RULE_7",
                    "Only Paid invoices can be refunded. Invoice status: " + originalInvoice.getStatus());
        }

        // Rule #4: Refund must be within 7 days of invoice creation
        long daysSinceSale = ChronoUnit.DAYS.between(originalInvoice.getCreatedAt(), LocalDateTime.now());
        if (daysSinceSale > 7) {
            throw new BusinessRuleViolationException("RULE_4",
                    String.format("Refund window expired. Invoice was created %d days ago (max 7 days allowed).",
                            daysSinceSale));
        }

        // Build a map: productId → InvoiceDetail for fast lookup
        Map<Long, InvoiceDetail> soldItems = originalInvoice.getDetails().stream()
                .collect(Collectors.toMap(InvoiceDetail::getProductId, d -> d));

        for (RefundItemRequest item : request.items()) {

            // Rule #5: Product must have been sold in original invoice
            InvoiceDetail soldDetail = soldItems.get(item.productId());
            if (soldDetail == null) {
                throw new BusinessRuleViolationException("RULE_5",
                        "Product id=" + item.productId() + " was not in the original invoice.");
            }

            // Rule #5: Cumulative refunded quantity + current request must not exceed sold quantity
            BigDecimal alreadyRefunded = refundDetailRepository
                    .sumRefundedQuantity(item.productId(), originalInvoice.getInvoiceId());
            BigDecimal totalRequested = alreadyRefunded.add(item.quantity());

            if (totalRequested.compareTo(soldDetail.getQuantity()) > 0) {
                throw new BusinessRuleViolationException("RULE_5",
                        String.format("Quantity exceeded for product id=%d: sold=%.3f, " +
                                      "already_refunded=%.3f, current_request=%.3f, max_allowed=%.3f",
                                item.productId(), soldDetail.getQuantity(),
                                alreadyRefunded, item.quantity(),
                                soldDetail.getQuantity().subtract(alreadyRefunded)));
            }
        }
    }
}


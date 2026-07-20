package com.retail.mapper;

import com.retail.entity.Product;
import com.retail.dto.InvoiceDetailResponse;
import com.retail.dto.InvoiceResponse;
import com.retail.entity.Invoice;
import com.retail.entity.InvoiceDetail;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Manual mapper (no MapStruct) — avoids reflection overhead and makes cross-module
 * join logic explicit and debuggable.
 *
 * Integration notes:
 *   - cashierName is resolved by service layer via real EmployeeRepository
 *   - sku, productName are joined from Product (replace with real Product at full merge)
 */
@Component
public class InvoiceMapper {

    /**
     * Map a single InvoiceDetail to response DTO.
     * productMap: productId → Product (pre-fetched batch in service layer)
     */
    public InvoiceDetailResponse toDetailResponse(InvoiceDetail detail,
                                                   Map<Long, Product> productMap,
                                                   Map<Long, BigDecimal> qtyAvailableMap) {
        Product product = productMap.get(detail.getProductId());
        String sku = product != null ? product.getSku() : "N/A";
        String productName = product != null ? product.getProductName() : "Unknown";
        BigDecimal qtyAvailable = qtyAvailableMap != null ? qtyAvailableMap.getOrDefault(detail.getProductId(), BigDecimal.ZERO) : BigDecimal.ZERO;

        // lineTotal: @Formula field may be null on newly-created (not yet flushed) entity
        BigDecimal lineTotal = detail.getLineTotal();
        if (lineTotal == null && detail.getUnitPrice() != null && detail.getQuantity() != null) {
            lineTotal = detail.getUnitPrice().multiply(detail.getQuantity());
        }

        return new InvoiceDetailResponse(
                detail.getInvoiceDetailId(),
                detail.getProductId(),
                sku,
                productName,
                detail.getQuantity(),
                detail.getUnitPrice(),
                detail.getPromotionId(),
                lineTotal,
                qtyAvailable
        );
    }

    /**
     * Map Invoice + cross-module lookups to full InvoiceResponse.
     * cashierName resolved by service from real EmployeeRepository.
     */
    public InvoiceResponse toResponse(Invoice invoice,
                                      Map<Long, Product> productMap,
                                      Map<Long, BigDecimal> qtyAvailableMap,
                                      String cashierName) {
        List<InvoiceDetailResponse> items = invoice.getDetails().stream()
                .map(d -> toDetailResponse(d, productMap, qtyAvailableMap))
                .toList();

        return new InvoiceResponse(
                invoice.getInvoiceId(),
                invoice.getInvoiceCode(),
                invoice.getBranchId(),
                invoice.getCashierId(),
                cashierName != null ? cashierName : "N/A",
                invoice.getStatus(),
                invoice.getPaymentMethod(),
                invoice.getTotalAmount(),
                invoice.getCreatedAt(),
                invoice.getPaidAt(),
                invoice.getCanceledAt(),
                items
        );
    }

    /**
     * Lightweight summary mapper (for list view — details not loaded).
     */
    public InvoiceResponse toSummaryResponse(Invoice invoice, String cashierName) {
        return new InvoiceResponse(
                invoice.getInvoiceId(),
                invoice.getInvoiceCode(),
                invoice.getBranchId(),
                invoice.getCashierId(),
                cashierName != null ? cashierName : "N/A",
                invoice.getStatus(),
                invoice.getPaymentMethod(),
                invoice.getTotalAmount(),
                invoice.getCreatedAt(),
                invoice.getPaidAt(),
                invoice.getCanceledAt(),
                List.of()
        );
    }
}


package com.retail.common.exception;

/**
 * Rule #1: Thrown when payment is attempted but BranchInventory.QtyAvailable
 * is insufficient for one or more invoice line items.
 */
public class InsufficientStockException extends BusinessRuleViolationException {

    private final Long productId;
    private final String productName;
    private final java.math.BigDecimal requested;
    private final java.math.BigDecimal available;

    public InsufficientStockException(Long productId, String productName,
                                      java.math.BigDecimal requested,
                                      java.math.BigDecimal available) {
        super("INSUFFICIENT_STOCK",
              String.format("Insufficient stock for product '%s' (id=%d): requested %.3f, available %.3f",
                            productName, productId, requested, available));
        this.productId = productId;
        this.productName = productName;
        this.requested = requested;
        this.available = available;
    }

    public Long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public java.math.BigDecimal getRequested() { return requested; }
    public java.math.BigDecimal getAvailable() { return available; }
}

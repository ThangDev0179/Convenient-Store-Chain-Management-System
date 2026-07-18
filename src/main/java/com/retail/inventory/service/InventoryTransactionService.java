package com.retail.inventory.service;

import com.retail.inventory.entity.TransactionType;

import java.math.BigDecimal;

public interface InventoryTransactionService {

    /**
     * Updates inventory and logs a transaction.
     *
     * @param branchId The ID of the branch.
     * @param productId The ID of the product.
     * @param qtyOnHandChange The change in quantity on hand (+/-).
     * @param qtyAvailableChange The change in available quantity (+/-).
     * @param qtyInTransitChange The change in in-transit quantity (+/-).
     * @param transactionType The type of transaction.
     * @param referenceTable The source table of the transaction (e.g., "StockTransfer").
     * @param referenceId The ID of the record in the source table.
     * @param reason Optional reason.
     * @param createdById ID of the employee performing the action.
     */
    void recordTransaction(
            Integer branchId,
            Long productId,
            BigDecimal qtyOnHandChange,
            BigDecimal qtyAvailableChange,
            BigDecimal qtyInTransitChange,
            TransactionType transactionType,
            String referenceTable,
            Long referenceId,
            String reason,
            Long createdById
    );
}

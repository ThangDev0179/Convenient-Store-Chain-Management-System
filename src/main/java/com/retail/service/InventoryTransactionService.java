package com.retail.service;
import com.retail.entity.InventoryTransactionType;
import com.retail.entity.StockTransfer;


import java.math.BigDecimal;

public interface InventoryTransactionService {

    /**
     * Ghi nhận giao dịch kho và cập nhật tồn kho tại chi nhánh.
     *
     * @param branchId            ID chi nhánh
     * @param productId           ID sản phẩm
     * @param qtyOnHandChange     Thay đổi QtyOnHand (+ thêm, - bớt)
     * @param qtyAvailableChange  Thay đổi QtyAvailable
     * @param qtyInTransitChange  Thay đổi QtyInTransit
     * @param transactionType     Loại giao dịch (enum InventoryTransactionType)
     * @param referenceTable      Tên bảng nguồn (VD: "StockTransfer")
     * @param referenceId         ID bản ghi nguồn
     * @param reason              Ghi chú lý do
     * @param createdById         EmployeeId thực hiện
     */
    void recordTransaction(
            Integer branchId,
            Long productId,
            BigDecimal qtyOnHandChange,
            BigDecimal qtyAvailableChange,
            BigDecimal qtyInTransitChange,
            InventoryTransactionType transactionType,
            String referenceTable,
            Long referenceId,
            String reason,
            Long createdById
    );
}
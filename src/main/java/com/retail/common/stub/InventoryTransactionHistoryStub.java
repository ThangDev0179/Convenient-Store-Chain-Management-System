package com.retail.common.stub;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * STUB — sẽ được thay bằng entity chính thức của thành viên 5 khi merge.
 * Sổ cái kho — PHẢI ghi mỗi khi bán (Sale) hoặc hoàn trả (Refund_Restock).
 *
 * Convention (đã chọn): Với RefundDetail.ConditionType='Damaged', ghi dòng ITH
 * với TransactionType='Refund_Restock', QuantityChange=0, Reason='Damaged - not restocked'.
 * Module Disposal (thành viên 5) sẽ xử lý xuất hủy sau.
 * QtyOnHand được cộng cho cả Damaged để phản ánh hàng thực có trong kho;
 * QtyAvailable chỉ cộng cho Resalable (hàng tốt mới được bán lại).
 */
@Entity
@Table(name = "InventoryTransactionHistory")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryTransactionHistoryStub {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TransactionId")
    private Long transactionId;

    @Column(name = "BranchId", nullable = false)
    private Integer branchId;

    @Column(name = "ProductId", nullable = false)
    private Long productId;

    /**
     * Allowed values: Sale | Refund_Restock | GRN | Disposal |
     *                 TransferOut | TransferIn | CountAdjustment
     */
    @Column(name = "TransactionType", length = 30, nullable = false)
    private String transactionType;

    @Column(name = "ReferenceTable", length = 50)
    private String referenceTable;

    @Column(name = "ReferenceId")
    private Long referenceId;

    /** Positive for inbound (+), negative for outbound (-). */
    @Column(name = "QuantityChange", precision = 18, scale = 3, nullable = false)
    private BigDecimal quantityChange;

    @Column(name = "Reason", length = 500)
    private String reason;

    @Column(name = "CreatedBy")
    private Long createdBy;

    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}

package com.retail.common.stub;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * STUB — sẽ được thay bằng entity chính thức của thành viên 3 khi merge.
 * Composite PK: (BranchId, ProductId).
 * CRITICAL: Đây là bảng quan trọng nhất cho khóa thanh toán POS —
 * QtyAvailable phải được check và trừ trong cùng một @Transactional khi pay().
 */
@Entity
@Table(name = "BranchInventory")
@Getter
@Setter
@NoArgsConstructor
@IdClass(BranchInventoryId.class)
public class BranchInventoryStub {

    @Id
    @Column(name = "BranchId")
    private Integer branchId;

    @Id
    @Column(name = "ProductId")
    private Long productId;

    @Column(name = "QtyOnHand", precision = 18, scale = 3)
    private BigDecimal qtyOnHand;

    @Column(name = "QtyAvailable", precision = 18, scale = 3)
    private BigDecimal qtyAvailable;

    @Column(name = "QtyInTransit", precision = 18, scale = 3)
    private BigDecimal qtyInTransit;

    @Column(name = "UpdatedAt")
    private LocalDateTime updatedAt;
}

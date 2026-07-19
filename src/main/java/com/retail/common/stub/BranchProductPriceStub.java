package com.retail.common.stub;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * STUB — sẽ được thay bằng entity chính thức của thành viên 2/3 khi merge.
 * Composite PK: (BranchId, ProductId).
 * EffectivePrice được ưu tiên sử dụng trước StandardPrice trong POS.
 */
@Entity
@Table(name = "BranchProductPrice")
@Getter
@Setter
@NoArgsConstructor
@IdClass(BranchProductPriceId.class)
public class BranchProductPriceStub {

    @Id
    @Column(name = "BranchId")
    private Integer branchId;

    @Id
    @Column(name = "ProductId")
    private Long productId;

    @Column(name = "EffectivePrice", precision = 18, scale = 2)
    private BigDecimal effectivePrice;

    @Column(name = "EffectiveFrom")
    private LocalDateTime effectiveFrom;
}

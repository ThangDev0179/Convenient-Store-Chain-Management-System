package com.retail.common.stub;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * STUB — sẽ được thay bằng entity chính thức của thành viên 2 khi merge.
 * Chỉ chứa các field tối thiểu cần thiết để compile và test module POS/Refund.
 */
@Entity
@Table(name = "Product")
@Getter
@Setter
@NoArgsConstructor
public class ProductStub {

    @Id
    @Column(name = "ProductId")
    private Long productId;

    @Column(name = "Sku", length = 50)
    private String sku;

    @Column(name = "ProductName", length = 255)
    private String productName;

    @Column(name = "CategoryId")
    private Long categoryId;

    @Column(name = "StandardPrice", precision = 18, scale = 2)
    private BigDecimal standardPrice;

    @Column(name = "Status", length = 20)
    private String status;
}

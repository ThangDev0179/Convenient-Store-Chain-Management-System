package com.retail.dto;

import com.retail.entity.ProductStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {
    private Long productId;
    private String sku;
    private String productName;
    private Integer categoryId;
    private String categoryName;
    private BigDecimal standardPrice;
    private Integer defaultSupplierId;
    private String defaultSupplierName;
    private ProductStatus status;
    private LocalDateTime createdAt;
}

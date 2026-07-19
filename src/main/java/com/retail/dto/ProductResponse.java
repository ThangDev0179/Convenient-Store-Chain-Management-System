package com.retail.dto;

import com.retail.entity.ProductStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {
    private Long productId;
    private String sku;
    private String barcode;
    private String productName;
    private String description;
    private Integer categoryId;
    private String categoryName;
    private BigDecimal standardPrice;
    private Integer defaultSupplierId;
    private String defaultSupplierName;
    private ProductStatus status;
    private List<UomResponse> uoms;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}

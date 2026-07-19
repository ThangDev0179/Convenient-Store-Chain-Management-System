package com.retail.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductCategoryResponse {
    private Integer categoryId;
    private String categoryName;
    private String skuPrefix;
}

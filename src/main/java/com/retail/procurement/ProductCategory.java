package com.retail.procurement;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ProductCategory")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CategoryId")
    private Integer categoryId;

    @Column(name = "CategoryName", nullable = false, length = 150)
    private String categoryName;

    @Column(name = "SkuPrefix", unique = true, nullable = false, length = 5)
    private String skuPrefix;
}

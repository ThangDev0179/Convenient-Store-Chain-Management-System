package com.retail.product;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Product")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ProductId")
    private Long productId;

    @Column(name = "Sku", nullable = false, unique = true, length = 20)
    private String sku;

    @Column(name = "ProductName", nullable = false, length = 200)
    private String productName;

    @Column(name = "CategoryId", nullable = false)
    private Integer categoryId;

    @Column(name = "StandardPrice", nullable = false, precision = 18, scale = 2)
    private BigDecimal standardPrice;

    @Column(name = "DefaultSupplierId")
    private Integer defaultSupplierId;

    @Column(name = "Status", nullable = false, length = 20)
    @Builder.Default
    private String status = "Active";

    @Column(name = "CreatedAt", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}

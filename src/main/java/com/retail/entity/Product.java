package com.retail.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Product")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ProductId")
    private Long productId;

    @Column(name = "Sku", unique = true, nullable = false, length = 20)
    private String sku;

    @Column(name = "ProductName", nullable = false, length = 200)
    private String productName;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "CategoryId", nullable = false)
    private ProductCategory category;

    @Column(name = "StandardPrice", nullable = false, precision = 18, scale = 2)
    private BigDecimal standardPrice;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "DefaultSupplierId")
    private Supplier defaultSupplier;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false, length = 20)
    private ProductStatus status;

    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = ProductStatus.Active;
        }
    }
}
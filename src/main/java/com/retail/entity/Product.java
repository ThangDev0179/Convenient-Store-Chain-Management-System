package com.retail.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Product")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ProductId")
    private Long productId;

    @Column(name = "Sku", unique = true, nullable = false, length = 20)
    private String sku;

    @Column(name = "Barcode", unique = true, length = 100)
    private String barcode;

    @Column(name = "ProductName", nullable = false, length = 150, columnDefinition = "NVARCHAR(150)")
    private String productName;

    @Column(name = "Description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "CategoryId", nullable = false)
    private ProductCategory category;

    @Builder.Default
    @Column(name = "StandardPrice", nullable = false, precision = 18, scale = 2)
    private BigDecimal standardPrice = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "DefaultSupplierId")
    private Supplier defaultSupplier;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private java.util.List<ProductUOM> uoms = new java.util.ArrayList<>();

    public void addUom(ProductUOM uom) {
        if (uoms == null) {
            uoms = new java.util.ArrayList<>();
        }
        uoms.add(uom);
        uom.setProduct(this);
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false, length = 20)
    private ProductStatus status;

    @CreatedDate
    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "UpdatedAt")
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(name = "CreatedBy", length = 100)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "UpdatedBy", length = 100)
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = ProductStatus.Active;
        }
        if (standardPrice == null) {
            standardPrice = BigDecimal.ZERO;
        }
    }
}
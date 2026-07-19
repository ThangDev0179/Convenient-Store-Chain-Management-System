package com.retail.entity;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ProductCategory")
@EntityListeners(AuditingEntityListener.class)
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

    @CreatedDate
    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "UpdatedAt")
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(name = "CreatedBy", length = 100, updatable = false)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "UpdatedBy", length = 100)
    private String updatedBy;
}
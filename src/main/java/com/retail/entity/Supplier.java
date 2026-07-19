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
@Table(name = "Supplier")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SupplierId")
    private Integer supplierId;

    @Column(name = "SupplierName", nullable = false, length = 200)
    private String supplierName;

    @Column(name = "ContactPhone", unique = true, length = 20)
    private String contactPhone;

    @Column(name = "ContactEmail", unique = true, length = 150)
    private String contactEmail;

    @Column(name = "Address", length = 300)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false, length = 20)
    private SupplierStatus status;

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

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = SupplierStatus.Active;
        }
    }
}
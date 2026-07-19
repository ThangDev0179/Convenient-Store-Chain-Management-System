package com.retail.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Supplier")
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

    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = SupplierStatus.Active;
        }
    }
}
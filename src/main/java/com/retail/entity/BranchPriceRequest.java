package com.retail.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "BranchPriceRequest")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchPriceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PriceRequestId")
    private Long priceRequestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BranchId", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProductId", nullable = false)
    private Product product;

    @Column(name = "ProposedPrice", nullable = false, precision = 18, scale = 2)
    private BigDecimal proposedPrice;

    @Column(name = "StandardPriceSnapshot", nullable = false, precision = 18, scale = 2)
    private BigDecimal standardPriceSnapshot;

    @Column(name = "Status", nullable = false, length = 20)
    private String status; // Pending | Approved | Rejected

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RequestedBy", nullable = false)
    private Employee requestedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ApprovedBy")
    private Employee approvedBy;

    @Column(name = "RequestedAt", nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    @Column(name = "ApprovedAt")
    private LocalDateTime approvedAt;

    @PrePersist
    protected void onCreate() {
        requestedAt = LocalDateTime.now();
        if (status == null) status = "Pending";
    }
}

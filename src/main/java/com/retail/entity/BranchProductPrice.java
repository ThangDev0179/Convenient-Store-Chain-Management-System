package com.retail.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "BranchProductPrice")
@IdClass(BranchProductPriceId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchProductPrice {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BranchId", nullable = false)
    private Branch branch;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProductId", nullable = false)
    private Product product;

    @Column(name = "EffectivePrice", nullable = false, precision = 18, scale = 2)
    private BigDecimal effectivePrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SourcePriceRequestId")
    private BranchPriceRequest sourcePriceRequest;

    @Column(name = "EffectiveFrom", nullable = false)
    private LocalDateTime effectiveFrom;

    @PrePersist
    protected void onCreate() {
        if (effectiveFrom == null) {
            effectiveFrom = LocalDateTime.now();
        }
    }
}

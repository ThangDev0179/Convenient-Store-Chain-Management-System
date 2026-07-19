package com.retail.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "StockDisposalDetail")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockDisposalDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DisposalDetailId")
    private Long disposalDetailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StockDisposalId", nullable = false)
    private StockDisposal stockDisposal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProductId", nullable = false)
    private Product product;

    @Column(name = "Quantity", nullable = false, precision = 18, scale = 3)
    private BigDecimal quantityDisposed;

    @Column(name = "UnitCost", nullable = false, precision = 18, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "Note", length = 200)
    private String note;
}
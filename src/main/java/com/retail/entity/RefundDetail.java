package com.retail.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "RefundDetail")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RefundDetailId")
    private Long refundDetailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RefundId", nullable = false)
    private Refund refund;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProductId", nullable = false)
    private Product product;

    @Column(name = "Quantity", nullable = false, precision = 18, scale = 3)
    private BigDecimal quantity;

    @Column(name = "ConditionType", nullable = false, length = 20)
    private String conditionType; // Damaged | Resalable

    @Column(name = "UnitRefundAmount", nullable = false, precision = 18, scale = 2)
    private BigDecimal unitRefundAmount;
}

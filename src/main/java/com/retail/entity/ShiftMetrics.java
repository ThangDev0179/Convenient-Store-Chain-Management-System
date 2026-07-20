package com.retail.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ShiftMetrics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftMetrics {

    @Id
    @Column(name = "WorkShiftId")
    private Long workShiftId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "WorkShiftId")
    private WorkShift workShift;

    @Column(name = "CashExpected", nullable = false, precision = 18, scale = 2)
    private BigDecimal cashExpected;

    @Column(name = "CashCounted", nullable = false, precision = 18, scale = 2)
    private BigDecimal cashCounted;

    @Column(name = "CashVariance", insertable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal cashVariance;

    @Column(name = "BankCardAmount", nullable = false, precision = 18, scale = 2)
    private BigDecimal bankCardAmount;

    @Column(name = "QrAmount", nullable = false, precision = 18, scale = 2)
    private BigDecimal qrAmount;

    @Column(name = "ClosedAt")
    private LocalDateTime closedAt;
}
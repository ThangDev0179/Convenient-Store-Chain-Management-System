package com.retail.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Refund")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RefundId")
    private Long refundId;

    @Column(name = "RefundCode", unique = true, nullable = false, length = 40)
    private String refundCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OriginalInvoiceId", nullable = false)
    private Invoice originalInvoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BranchId", nullable = false)
    private Branch branch;

    @Column(name = "CustomerName", nullable = false, length = 150)
    private String customerName;

    @Column(name = "CustomerPhone", nullable = false, length = 20)
    private String customerPhone;

    @Column(name = "Reason", nullable = false, length = 500)
    private String reason;

    @Column(name = "TotalRefundAmount", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalRefundAmount;

    @Column(name = "Status", nullable = false, length = 20)
    private String status; // Draft | Pending_Approval | Completed | Rejected

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RequestedBy", nullable = false)
    private Employee requestedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ApprovedBy")
    private Employee approvedBy;

    @Column(name = "PinOverrideUsed", nullable = false)
    @Builder.Default
    private Boolean pinOverrideUsed = false;

    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "ApprovedAt")
    private LocalDateTime approvedAt;

    @OneToMany(mappedBy = "refund", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<RefundDetail> details = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = "Draft";
        if (pinOverrideUsed == null) pinOverrideUsed = false;
    }

    public void addDetail(RefundDetail detail) {
        details.add(detail);
        detail.setRefund(this);
    }
}

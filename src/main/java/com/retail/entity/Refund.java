package com.retail.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Maps to dbo.Refund — DO NOT rename any column.
 */
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

    /**
     * Format: REF-[BranchCode]-YYYYMMDD-[4 số tự tăng].
     * Generated in service layer.
     */
    @Column(name = "RefundCode", length = 40, nullable = false, unique = true, updatable = false)
    private String refundCode;

    /**
     * FK → Invoice. The original paid invoice being refunded.
     * Rule #7: only Paid invoices can be refunded.
     */
    @Column(name = "OriginalInvoiceId", nullable = false)
    private Long originalInvoiceId;

    /**
     * Rule #6: must match Invoice.BranchId of the original invoice — set by service, not client.
     */
    @Column(name = "BranchId", nullable = false)
    private Integer branchId;

    @Column(name = "CustomerName", length = 150, nullable = false)
    private String customerName;

    @Column(name = "CustomerPhone", length = 20, nullable = false)
    private String customerPhone;

    @Column(name = "Reason", length = 500, nullable = false)
    private String reason;

    @Column(name = "TotalRefundAmount", precision = 18, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal totalRefundAmount = BigDecimal.ZERO;

    /**
     * State: Draft → Pending_Approval → Completed | Rejected.
     * Auto-set by service based on TotalRefundAmount threshold (≥200,000 VND → Pending_Approval).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "Status", columnDefinition = "NVARCHAR(20) CHECK (Status IN ('Draft','Pending_Approval','Completed','Rejected'))", nullable = false)
    @Builder.Default
    private RefundStatus status = RefundStatus.Draft;

    /** FK → Employee. Set to currently logged-in employee (not client-provided). */
    @Column(name = "RequestedBy", nullable = false)
    private Long requestedBy;

    /** FK → Employee (Manager). Nullable until approved. */
    @Column(name = "ApprovedBy")
    private Long approvedBy;

    /**
     * True when Manager physically at POS uses PIN override to approve immediately.
     * See 3.2.2 override-approve endpoint.
     */
    @Column(name = "PinOverrideUsed", nullable = false)
    @Builder.Default
    private boolean pinOverrideUsed = false;

    @Column(name = "CreatedAt", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "ApprovedAt")
    private LocalDateTime approvedAt;

    @OneToMany(mappedBy = "refund", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<RefundDetail> details = new ArrayList<>();

    public void addDetail(RefundDetail detail) {
        detail.setRefund(this);
        this.details.add(detail);
    }

    public void recalculateTotalRefundAmount() {
        this.totalRefundAmount = details.stream()
                .map(d -> d.getUnitRefundAmount().multiply(d.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

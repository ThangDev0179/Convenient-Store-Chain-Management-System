package com.retail.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "StockDisposal")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"details", "branch", "createdBy", "approvedBy"})
@EqualsAndHashCode(exclude = {"details"})
public class StockDisposal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "StockDisposalId")
    private Long disposalId;

    @Column(name = "DisposalCode", unique = true, nullable = false, length = 50)
    private String disposalCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BranchId", nullable = false)
    private Branch branch;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false, length = 20)
    private StockDisposalStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "SourceType", nullable = false, length = 20)
    private DisposalSourceType sourceType;

    @Column(name = "RelatedTransferId")
    private Long referenceId;

    @Column(name = "Reason", length = 500)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CreatedBy", nullable = false)
    private Employee createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ApprovedBy")
    private Employee approvedBy;

    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "ApprovedAt")
    private LocalDateTime approvedAt;

    @Builder.Default
    @OneToMany(mappedBy = "stockDisposal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StockDisposalDetail> details = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = StockDisposalStatus.Draft;
    }

    public void addDetail(StockDisposalDetail detail) {
        details.add(detail);
        detail.setStockDisposal(this);
    }
}
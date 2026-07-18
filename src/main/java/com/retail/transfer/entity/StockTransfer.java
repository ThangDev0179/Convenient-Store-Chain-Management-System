package com.retail.transfer.entity;

import com.retail.branch.Branch;
import com.retail.employee.Employee;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "StockTransfer")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "StockTransferId")
    private Long stockTransferId;

    @Column(name = "TransferCode", nullable = false, unique = true, length = 50)
    private String transferCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FromBranchId", nullable = false)
    @ToString.Exclude
    private Branch fromBranch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ToBranchId", nullable = false)
    @ToString.Exclude
    private Branch toBranch;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false, length = 20)
    @Builder.Default
    private StockTransferStatus status = StockTransferStatus.Draft;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CreatedBy", nullable = false)
    @ToString.Exclude
    private Employee createdBy;

    @Column(name = "CreatedAt", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ReceivedBy")
    @ToString.Exclude
    private Employee receivedBy;

    @Column(name = "ReceivedAt")
    private LocalDateTime receivedAt;

    @OneToMany(mappedBy = "stockTransfer", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    private List<StockTransferDetail> details = new ArrayList<>();

    public void addDetail(StockTransferDetail detail) {
        details.add(detail);
        detail.setStockTransfer(this);
    }
}

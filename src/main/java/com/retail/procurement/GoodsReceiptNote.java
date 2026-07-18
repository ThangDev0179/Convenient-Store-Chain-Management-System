package com.retail.procurement;

import com.retail.branch.Branch;
import com.retail.employee.Employee;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "GoodsReceiptNote")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsReceiptNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "GrnId")
    private Long grnId;

    @Column(name = "GrnCode", unique = true, nullable = false, length = 40)
    private String grnCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PurchaseOrderId", nullable = false)
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BranchId", nullable = false)
    private Branch branch;

    @Column(name = "ReceivedAt")
    private LocalDateTime receivedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ReceivedBy", nullable = false)
    private Employee receivedBy;

    @Column(name = "Status", nullable = false, length = 30)
    private String status; // Completed, Canceled

    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "goodsReceiptNote", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<GoodsReceiptNoteDetail> details = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (receivedAt == null) {
            receivedAt = LocalDateTime.now();
        }
    }
}

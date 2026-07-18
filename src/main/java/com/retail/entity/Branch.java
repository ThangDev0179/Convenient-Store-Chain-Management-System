package com.retail.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "[Branch]")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BranchId")
    private Integer branchId;

    @Column(name = "BranchCode", unique = true, nullable = false, length = 10)
    private String branchCode;

    @Column(name = "BranchName", nullable = false, length = 200)
    private String branchName;

    @Column(name = "Address", length = 300)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false, length = 20)
    private BranchStatus status;

    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "ArchivedAt")
    private LocalDateTime archivedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = BranchStatus.Active;
        }
    }
}

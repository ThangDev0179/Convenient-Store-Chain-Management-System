package com.retail.branch.dto;

import com.retail.branch.BranchStatus;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchResponse {
    private Integer branchId;
    private String branchCode;
    private String branchName;
    private String address;
    private BranchStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime archivedAt;
    private Long managerId;
    private String managerName;
}

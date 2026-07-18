package com.retail.employee.dto;

import com.retail.employee.EmployeeStatus;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeResponse {
    private Long employeeId;
    private String employeeCode;
    private String username;
    private String fullName;
    private String email;
    private String phone;
    private Integer roleId;
    private String roleName;
    private String roleCode;
    private Integer branchId;
    private String branchName;
    private Boolean isBranchManager;
    private EmployeeStatus status;
    private Boolean forceChangePassword;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

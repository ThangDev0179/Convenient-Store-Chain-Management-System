package com.retail.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateEmployeeRequest {
    private String fullName;
    private String email;
    private String phone;
    private Long roleId;
    private Integer branchId;
    private Boolean isBranchManager;
}

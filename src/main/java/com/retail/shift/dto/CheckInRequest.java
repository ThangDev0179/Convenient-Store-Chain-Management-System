package com.retail.shift.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckInRequest {
    private Long employeeId;
    private Integer branchId;
}

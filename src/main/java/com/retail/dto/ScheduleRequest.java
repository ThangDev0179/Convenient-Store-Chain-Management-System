package com.retail.dto;

import lombok.*;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleRequest {
    private Long employeeId;
    private Integer branchId;
    private Integer shiftTypeId;
    private Integer dayOfWeek; // 1-7
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
}
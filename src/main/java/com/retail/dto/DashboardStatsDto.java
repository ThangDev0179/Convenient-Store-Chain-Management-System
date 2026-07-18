package com.retail.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsDto {
    private long totalEmployees;
    private long totalBranches;
    private long openShiftsCount;
    private BigDecimal todayRevenue;
    private List<String> recentNotifications;
}

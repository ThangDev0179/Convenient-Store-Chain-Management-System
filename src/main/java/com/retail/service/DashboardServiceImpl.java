package com.retail.service;

import com.retail.dto.DashboardStatsDto;
import com.retail.entity.BranchStatus;
import com.retail.entity.EmployeeStatus;
import com.retail.entity.WorkShiftStatus;
import com.retail.repository.BranchRepository;
import com.retail.repository.EmployeeRepository;
import com.retail.repository.WorkShiftRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private WorkShiftRepository workShiftRepository;

    @Autowired
    private DashboardStatsProvider statsProvider;

    @Override
    public DashboardStatsDto getStats() {
        long activeEmployees = employeeRepository.countByStatus(EmployeeStatus.Active);
        long activeBranches = branchRepository.countByStatus(BranchStatus.Active);
        long openShifts = workShiftRepository.countByStatus(WorkShiftStatus.Open);

        List<String> notifications = new ArrayList<>();
        notifications.add("Hệ thống bán lẻ hoạt động ổn định.");
        notifications.add("Cơ sở dữ liệu SQL Server đã được đồng bộ hóa.");
        if (openShifts > 0) {
            notifications.add("Đang có " + openShifts + " ca làm việc đang mở trực tuyến.");
        } else {
            notifications.add("Hiện tại không có ca làm việc nào đang mở.");
        }

        return DashboardStatsDto.builder()
                .totalEmployees(activeEmployees)
                .totalBranches(activeBranches)
                .openShiftsCount(openShifts)
                .todayRevenue(statsProvider.getTodayRevenue())
                .recentNotifications(notifications)
                .build();
    }
}

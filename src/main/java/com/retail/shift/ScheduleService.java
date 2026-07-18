package com.retail.shift;

import com.retail.shift.dto.ScheduleRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ScheduleService {
    EmployeeShiftSchedule create(ScheduleRequest request);
    EmployeeShiftSchedule update(Long scheduleId, ScheduleRequest request);
    void delete(Long scheduleId);
    Page<EmployeeShiftSchedule> list(String search, Integer branchId, Long employeeId, Pageable pageable);
    EmployeeShiftSchedule getDetail(Long scheduleId);
}

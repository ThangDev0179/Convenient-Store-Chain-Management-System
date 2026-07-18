package com.retail.service;

import com.retail.dto.ScheduleRequest;
import com.retail.entity.Branch;
import com.retail.entity.Employee;
import com.retail.entity.EmployeeShiftSchedule;
import com.retail.entity.ShiftType;
import com.retail.exception.ValidationException;
import com.retail.repository.BranchRepository;
import com.retail.repository.EmployeeRepository;
import com.retail.repository.EmployeeShiftScheduleRepository;
import com.retail.repository.ShiftTypeRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class ScheduleServiceImpl implements ScheduleService {

    @Autowired
    private EmployeeShiftScheduleRepository scheduleRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private ShiftTypeRepository shiftTypeRepository;

    @Override
    public EmployeeShiftSchedule create(ScheduleRequest request) {
        validateDates(request);

        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ValidationException("Nhân viên không tồn tại"));

        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new ValidationException("Chi nhánh không tồn tại"));

        ShiftType shiftType = shiftTypeRepository.findById(request.getShiftTypeId())
                .orElseThrow(() -> new ValidationException("Ca làm việc không tồn tại"));

        // Overlapping Check
        List<EmployeeShiftSchedule> overlaps = scheduleRepository.findOverlappingSchedules(
                request.getEmployeeId(), request.getDayOfWeek(), request.getEffectiveFrom(), request.getEffectiveTo());

        if (!overlaps.isEmpty()) {
            throw new ValidationException("Lịch làm việc bị trùng lặp thời gian cho nhân viên này vào thứ " + getDayOfWeekName(request.getDayOfWeek()) + ".");
        }

        EmployeeShiftSchedule schedule = EmployeeShiftSchedule.builder()
                .employee(employee)
                .branch(branch)
                .shiftType(shiftType)
                .dayOfWeek(request.getDayOfWeek())
                .effectiveFrom(request.getEffectiveFrom())
                .effectiveTo(request.getEffectiveTo())
                .build();

        return scheduleRepository.save(schedule);
    }

    @Override
    public EmployeeShiftSchedule update(Long scheduleId, ScheduleRequest request) {
        EmployeeShiftSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ValidationException("Lịch làm việc không tồn tại"));

        validateDates(request);

        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ValidationException("Nhân viên không tồn tại"));

        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new ValidationException("Chi nhánh không tồn tại"));

        ShiftType shiftType = shiftTypeRepository.findById(request.getShiftTypeId())
                .orElseThrow(() -> new ValidationException("Ca làm việc không tồn tại"));

        // Overlapping Check excluding current ID
        List<EmployeeShiftSchedule> overlaps = scheduleRepository.findOverlappingSchedulesExcludingId(
                request.getEmployeeId(), request.getDayOfWeek(), request.getEffectiveFrom(), request.getEffectiveTo(), scheduleId);

        if (!overlaps.isEmpty()) {
            throw new ValidationException("Lịch làm việc bị trùng lặp thời gian cho nhân viên này vào thứ " + getDayOfWeekName(request.getDayOfWeek()) + ".");
        }

        schedule.setEmployee(employee);
        schedule.setBranch(branch);
        schedule.setShiftType(shiftType);
        schedule.setDayOfWeek(request.getDayOfWeek());
        schedule.setEffectiveFrom(request.getEffectiveFrom());
        schedule.setEffectiveTo(request.getEffectiveTo());

        return scheduleRepository.save(schedule);
    }

    @Override
    public void delete(Long scheduleId) {
        if (!scheduleRepository.existsById(scheduleId)) {
            throw new ValidationException("Lịch làm việc không tồn tại");
        }
        scheduleRepository.deleteById(scheduleId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeShiftSchedule> list(String search, Integer branchId, Long employeeId, Pageable pageable) {
        Specification<EmployeeShiftSchedule> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (branchId != null) {
                predicates.add(cb.equal(root.get("branch").get("branchId"), branchId));
            }
            if (employeeId != null) {
                predicates.add(cb.equal(root.get("employee").get("employeeId"), employeeId));
            }

            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";
                Predicate employeeNamePred = cb.like(cb.lower(root.get("employee").get("fullName")), searchPattern);
                Predicate employeeCodePred = cb.like(cb.lower(root.get("employee").get("employeeCode")), searchPattern);
                Predicate shiftNamePred = cb.like(cb.lower(root.get("shiftType").get("shiftName")), searchPattern);
                predicates.add(cb.or(employeeNamePred, employeeCodePred, shiftNamePred));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return scheduleRepository.findAll(spec, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeShiftSchedule getDetail(Long scheduleId) {
        return scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ValidationException("Lịch làm việc không tồn tại"));
    }

    private void validateDates(ScheduleRequest request) {
        if (request.getEmployeeId() == null || request.getBranchId() == null || request.getShiftTypeId() == null) {
            throw new ValidationException("Nhân viên, chi nhánh và ca làm việc không được để trống");
        }
        if (request.getDayOfWeek() == null || request.getDayOfWeek() < 1 || request.getDayOfWeek() > 7) {
            throw new ValidationException("Ngày trong tuần không hợp lệ (phải từ 1 đến 7)");
        }
        if (request.getEffectiveFrom() == null) {
            throw new ValidationException("Ngày hiệu lực bắt đầu không được để trống");
        }
        if (request.getEffectiveTo() != null && request.getEffectiveTo().isBefore(request.getEffectiveFrom())) {
            throw new ValidationException("Ngày hiệu lực kết thúc phải bằng hoặc sau ngày bắt đầu");
        }
    }

    private String getDayOfWeekName(int day) {
        switch (day) {
            case 1: return "Hai";
            case 2: return "Ba";
            case 3: return "Tư";
            case 4: return "Năm";
            case 5: return "Sáu";
            case 6: return "Bảy";
            case 7: return "Chủ Nhật";
            default: return "";
        }
    }
}

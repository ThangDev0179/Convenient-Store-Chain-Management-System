package com.retail.service.impl;
import com.retail.exception.AlreadyCheckedInException;
import com.retail.entity.Branch;
import com.retail.repository.BranchRepository;
import com.retail.dto.CashClosingRequest;
import com.retail.dto.CheckInRequest;
import com.retail.dto.CheckOutRequest;
import com.retail.entity.Employee;
import com.retail.repository.EmployeeRepository;
import com.retail.entity.ShiftMetrics;
import com.retail.repository.ShiftMetricsRepository;
import com.retail.exception.ValidationException;
import com.retail.entity.WorkShift;
import com.retail.repository.WorkShiftRepository;
import com.retail.service.WorkShiftService;
import com.retail.entity.WorkShiftStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class WorkShiftServiceImpl implements WorkShiftService {

    @Autowired
    private WorkShiftRepository workShiftRepository;

    @Autowired
    private ShiftMetricsRepository shiftMetricsRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private BranchRepository branchRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public WorkShift checkIn(CheckInRequest request) {
        if (request.getEmployeeId() == null || request.getBranchId() == null) {
            throw new ValidationException("Mã nhân viên và mã chi nhánh không được để trống");
        }

        boolean hasOpen = workShiftRepository.existsByEmployeeEmployeeIdAndStatus(request.getEmployeeId(), WorkShiftStatus.Open);
        if (hasOpen) {
            throw new AlreadyCheckedInException("Bạn đã check-in ca làm việc rồi, chưa check-out ca trước đó");
        }

        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ValidationException("Nhân viên không tồn tại"));

        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new ValidationException("Chi nhánh không tồn tại"));

        WorkShift workShift = WorkShift.builder()
                .employee(employee)
                .branch(branch)
                .checkInTime(LocalDateTime.now())
                .status(WorkShiftStatus.Open)
                .build();

        return workShiftRepository.save(workShift);
    }

    @Override
    public WorkShift checkOut(CheckOutRequest request) {
        if (request.getWorkShiftId() == null) {
            throw new ValidationException("Mã ca làm việc không được để trống");
        }

        WorkShift workShift = workShiftRepository.findById(request.getWorkShiftId())
                .orElseThrow(() -> new ValidationException("Ca làm việc không tồn tại"));

        if (workShift.getStatus() != WorkShiftStatus.Open) {
            throw new ValidationException("Ca làm việc đã được đóng trước đó");
        }

        LocalDateTime checkOutTime = LocalDateTime.now();
        if (checkOutTime.isBefore(workShift.getCheckInTime())) {
            throw new ValidationException("Thời gian check-out phải sau thời gian check-in");
        }

        workShift.setCheckOutTime(checkOutTime);
        workShift.setStatus(WorkShiftStatus.Closed);

        return workShiftRepository.save(workShift);
    }

    @Override
    public void cashClosing(CashClosingRequest request) {
        if (request.getWorkShiftId() == null) {
            throw new ValidationException("Mã ca làm việc không được để trống");
        }
        if (request.getCashCounted() == null || request.getBankCardAmount() == null || request.getQrAmount() == null) {
            throw new ValidationException("Số tiền mặt thực tế, thẻ ngân hàng và chuyển khoản QR không được để trống");
        }

        WorkShift workShift = workShiftRepository.findById(request.getWorkShiftId())
                .orElseThrow(() -> new ValidationException("Ca làm việc không tồn tại"));

        if (workShift.getStatus() == WorkShiftStatus.Open) {
            throw new ValidationException("Vui lòng check-out ca làm việc trước khi thực hiện kết ca kiểm tiền.");
        }

        BigDecimal cashExpected = calculateExpectedCashFromDB(workShift);
        BigDecimal cashCounted = request.getCashCounted();
        BigDecimal cashVariance = cashCounted.subtract(cashExpected);

        if (cashVariance.compareTo(BigDecimal.ZERO) != 0) {
            workShift.setStatus(WorkShiftStatus.Warning_Mismatch);
        } else {
            workShift.setStatus(WorkShiftStatus.Closed);
        }
        workShiftRepository.save(workShift);

        ShiftMetrics metrics = ShiftMetrics.builder()
                .workShiftId(workShift.getWorkShiftId())
                .workShift(workShift)
                .cashExpected(cashExpected)
                .cashCounted(cashCounted)
                .cashVariance(cashVariance)
                .bankCardAmount(request.getBankCardAmount())
                .qrAmount(request.getQrAmount())
                .closedAt(LocalDateTime.now())
                .build();

        shiftMetricsRepository.save(metrics);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WorkShift> list(String search, Integer branchId, Long employeeId, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        Specification<WorkShift> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (branchId != null) {
                predicates.add(cb.equal(root.get("branch").get("branchId"), branchId));
            }
            if (employeeId != null) {
                predicates.add(cb.equal(root.get("employee").get("employeeId"), employeeId));
            }

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("checkInTime"), LocalDateTime.of(startDate, LocalTime.MIN)));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("checkInTime"), LocalDateTime.of(endDate, LocalTime.MAX)));
            }

            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";
                Predicate employeeName = cb.like(cb.lower(root.get("employee").get("fullName")), searchPattern);
                Predicate employeeCode = cb.like(cb.lower(root.get("employee").get("employeeCode")), searchPattern);
                predicates.add(cb.or(employeeName, employeeCode));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return workShiftRepository.findAll(spec, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkShift getDetail(Long workShiftId) {
        return workShiftRepository.findById(workShiftId)
                .orElseThrow(() -> new ValidationException("Ca làm việc không tồn tại"));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WorkShift> findActiveShiftForEmployee(Long employeeId) {
        return workShiftRepository.findFirstByEmployeeEmployeeIdAndStatus(employeeId, WorkShiftStatus.Open);
    }

    private BigDecimal calculateExpectedCashFromDB(WorkShift workShift) {
        BigDecimal baseCash = BigDecimal.valueOf(1000000.0);

        try {
            Query query = entityManager.createNativeQuery(
                    "SELECT SUM(TotalAmount) FROM [Invoice] " +
                    "WHERE CashierId = :cashierId " +
                    "AND CreatedAt >= :checkIn " +
                    "AND CreatedAt <= :checkOut " +
                    "AND PaymentMethod = 'Cash' " +
                    "AND Status = 'Paid'"
            );
            query.setParameter("cashierId", workShift.getEmployee().getEmployeeId());
            query.setParameter("checkIn", workShift.getCheckInTime());
            query.setParameter("checkOut", workShift.getCheckOutTime());

            Object result = query.getSingleResult();
            if (result != null) {
                BigDecimal sum = new BigDecimal(result.toString());
                return baseCash.add(sum);
            }
        } catch (Exception ignored) {
        }

        return baseCash;
    }
}
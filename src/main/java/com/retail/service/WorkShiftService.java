package com.retail.service;
import com.retail.dto.CashClosingRequest;
import com.retail.dto.CheckInRequest;
import com.retail.dto.CheckOutRequest;
import com.retail.entity.WorkShift;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.util.Optional;

public interface WorkShiftService {
    WorkShift checkIn(CheckInRequest request);
    WorkShift checkOut(CheckOutRequest request);
    void cashClosing(CashClosingRequest request);
    Page<WorkShift> list(String search, Integer branchId, Long employeeId, LocalDate startDate, LocalDate endDate, Pageable pageable);
    WorkShift getDetail(Long workShiftId);
    Optional<WorkShift> findActiveShiftForEmployee(Long employeeId);
}
package com.retail.service;

import com.retail.dto.*;
import com.retail.entity.EmployeeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EmployeeService {
    EmployeeResponse create(CreateEmployeeRequest request);
    EmployeeResponse update(Long employeeId, UpdateEmployeeRequest request);
    void lock(Long employeeId);
    void unlock(Long employeeId);
    void resetPassword(Long employeeId);
    void changeRole(Long employeeId, ChangeRoleRequest request);
    void transferBranch(Long employeeId, TransferBranchRequest request);
    Page<EmployeeResponse> list(String search, Integer branchId, Long roleId, EmployeeStatus status, Pageable pageable);
    EmployeeResponse getDetail(Long employeeId);
}

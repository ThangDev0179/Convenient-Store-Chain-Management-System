package com.retail.service;
import com.retail.dto.ChangeRoleRequest;
import com.retail.dto.CreateEmployeeRequest;
import com.retail.dto.EmployeeResponse;
import com.retail.entity.EmployeeStatus;
import com.retail.dto.TransferBranchRequest;
import com.retail.dto.UpdateEmployeeRequest;

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
    Page<EmployeeResponse> list(String search, Integer branchId, Integer roleId, EmployeeStatus status, Pageable pageable);
    EmployeeResponse getDetail(Long employeeId);
}
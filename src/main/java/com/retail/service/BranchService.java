package com.retail.service;
import com.retail.dto.BranchResponse;
import com.retail.entity.BranchStatus;
import com.retail.dto.CreateBranchRequest;
import com.retail.dto.UpdateBranchRequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BranchService {
    BranchResponse create(CreateBranchRequest request);
    BranchResponse update(Integer branchId, UpdateBranchRequest request);
    void archive(Integer branchId);
    void close(Integer branchId);
    void reopen(Integer branchId);
    Page<BranchResponse> list(String search, BranchStatus status, Pageable pageable);
    BranchResponse getDetail(Integer branchId);
}
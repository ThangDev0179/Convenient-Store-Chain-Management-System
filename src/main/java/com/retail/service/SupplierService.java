package com.retail.service;

import com.retail.dto.SupplierRequest;
import com.retail.dto.SupplierResponse;
import com.retail.entity.SupplierStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SupplierService {
    Page<SupplierResponse> list(String search, SupplierStatus status, Pageable pageable);
    SupplierResponse getDetail(Integer id);
    SupplierResponse create(SupplierRequest request);
    SupplierResponse update(Integer id, SupplierRequest request);
    void delete(Integer id);
    void restore(Integer id);
}

package com.retail.service;

import com.retail.dto.ProductRequest;
import com.retail.dto.ProductResponse;
import com.retail.entity.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {
    Page<ProductResponse> list(String search, Integer categoryId, Integer supplierId, ProductStatus status, Pageable pageable);
    ProductResponse getDetail(Long id);
    void create(ProductRequest request);
    void update(Long id, ProductRequest request);
    void delete(Long id);
    void restore(Long id);
    int calculateBaseQuantity(Long uomId, int transactionQuantity);
}

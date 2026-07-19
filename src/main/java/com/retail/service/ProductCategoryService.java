package com.retail.service;

import com.retail.dto.ProductCategoryRequest;
import com.retail.dto.ProductCategoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductCategoryService {
    Page<ProductCategoryResponse> list(String search, Pageable pageable);
    ProductCategoryResponse getDetail(Integer id);
    ProductCategoryResponse create(ProductCategoryRequest request);
    ProductCategoryResponse update(Integer id, ProductCategoryRequest request);
    void delete(Integer id);
}

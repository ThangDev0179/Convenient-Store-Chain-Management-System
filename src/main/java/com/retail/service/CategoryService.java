package com.retail.service;

import com.retail.dto.CategoryRequest;
import com.retail.dto.CategoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CategoryService {
    Page<CategoryResponse> list(String search, Pageable pageable);
    CategoryResponse getDetail(Integer id);
    CategoryResponse create(CategoryRequest request);
    CategoryResponse update(Integer id, CategoryRequest request);
    void delete(Integer id);
}

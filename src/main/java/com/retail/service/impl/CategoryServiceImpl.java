package com.retail.service.impl;

import com.retail.dto.CategoryRequest;
import com.retail.dto.CategoryResponse;
import com.retail.entity.ProductCategory;
import com.retail.exception.ValidationException;
import com.retail.repository.ProductCategoryRepository;
import com.retail.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private ProductCategoryRepository categoryRepository;

    @Override
    public Page<CategoryResponse> list(String search, Pageable pageable) {
        String keyword = (search == null || search.trim().isEmpty()) ? null : search.trim();
        return categoryRepository.searchCategories(keyword, pageable)
                .map(this::mapToResponse);
    }

    @Override
    public CategoryResponse getDetail(Integer id) {
        ProductCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Không tìm thấy danh mục ngành hàng với ID: " + id));
        return mapToResponse(category);
    }

    @Override
    public CategoryResponse create(CategoryRequest request) {
        validateCategory(null, request);

        ProductCategory category = ProductCategory.builder()
                .categoryName(request.getCategoryName().trim())
                .skuPrefix(request.getSkuPrefix().trim().toUpperCase())
                .build();

        category = categoryRepository.save(category);
        return mapToResponse(category);
    }

    @Override
    public CategoryResponse update(Integer id, CategoryRequest request) {
        ProductCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Không tìm thấy danh mục ngành hàng với ID: " + id));

        validateCategory(id, request);

        category.setCategoryName(request.getCategoryName().trim());
        category.setSkuPrefix(request.getSkuPrefix().trim().toUpperCase());

        category = categoryRepository.save(category);
        return mapToResponse(category);
    }

    @Override
    public void delete(Integer id) {
        ProductCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Không tìm thấy danh mục ngành hàng với ID: " + id));
        try {
            categoryRepository.deleteById(id);
            // Flush to trigger DB constraints check inside transaction boundaries
            categoryRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ValidationException("Không thể xóa danh mục này vì đang có sản phẩm (Product) liên kết.");
        }
    }

    private void validateCategory(Integer id, CategoryRequest request) {
        String prefix = request.getSkuPrefix() != null ? request.getSkuPrefix().trim().toUpperCase() : null;

        if (prefix != null && !prefix.isEmpty()) {
            boolean prefixExists = (id == null)
                    ? categoryRepository.existsBySkuPrefix(prefix)
                    : categoryRepository.existsBySkuPrefixAndCategoryIdNot(prefix, id);
            if (prefixExists) {
                throw new ValidationException("Tiền tố SKU đã tồn tại trong hệ thống");
            }
        }
    }

    private CategoryResponse mapToResponse(ProductCategory category) {
        return CategoryResponse.builder()
                .categoryId(category.getCategoryId())
                .categoryName(category.getCategoryName())
                .skuPrefix(category.getSkuPrefix())
                .build();
    }
}

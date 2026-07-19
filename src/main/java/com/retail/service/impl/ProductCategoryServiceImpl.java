package com.retail.service.impl;

import com.retail.dto.ProductCategoryRequest;
import com.retail.dto.ProductCategoryResponse;
import com.retail.entity.ProductCategory;
import com.retail.exception.ValidationException;
import com.retail.repository.ProductCategoryRepository;
import com.retail.repository.ProductRepository;
import com.retail.service.ProductCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProductCategoryServiceImpl implements ProductCategoryService {

    @Autowired
    private ProductCategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ProductCategoryResponse> list(String search, Pageable pageable) {
        String keyword = (search == null || search.trim().isEmpty()) ? null : search.trim();
        return categoryRepository.searchCategories(keyword, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductCategoryResponse getDetail(Integer id) {
        ProductCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Không tìm thấy danh mục ngành hàng với ID: " + id));
        return mapToResponse(category);
    }

    @Override
    public ProductCategoryResponse create(ProductCategoryRequest request) {
        validateCategory(null, request);

        ProductCategory category = ProductCategory.builder()
                .categoryName(request.getCategoryName().trim())
                .skuPrefix(request.getSkuPrefix().trim().toUpperCase())
                .build();

        try {
            category = categoryRepository.save(category);
            // Flush to trigger DB constraints immediately inside transaction bounds for race condition check
            categoryRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ValidationException("Tiền tố SKU đã tồn tại trong hệ thống");
        }

        return mapToResponse(category);
    }

    @Override
    public ProductCategoryResponse update(Integer id, ProductCategoryRequest request) {
        ProductCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Không tìm thấy danh mục ngành hàng với ID: " + id));

        validateCategory(id, request);

        category.setCategoryName(request.getCategoryName().trim());
        category.setSkuPrefix(request.getSkuPrefix().trim().toUpperCase());

        try {
            category = categoryRepository.save(category);
            categoryRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ValidationException("Tiền tố SKU đã tồn tại trong hệ thống");
        }

        return mapToResponse(category);
    }

    @Override
    public void delete(Integer id) {
        ProductCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Không tìm thấy danh mục ngành hàng với ID: " + id));

        // Proactive foreign key check
        if (productRepository.existsByCategoryId(id)) {
            throw new ValidationException("Không thể xóa danh mục này vì đang có sản phẩm liên kết");
        }

        categoryRepository.deleteById(id);
        categoryRepository.flush();
    }

    private void validateCategory(Integer id, ProductCategoryRequest request) {
        String name = request.getCategoryName() != null ? request.getCategoryName().trim() : null;
        if (name != null && !name.isEmpty()) {
            boolean nameExists = (id == null)
                    ? categoryRepository.existsByCategoryName(name)
                    : categoryRepository.existsByCategoryNameAndCategoryIdNot(name, id);
            if (nameExists) {
                throw new ValidationException("Tên danh mục ngành hàng đã tồn tại");
            }
        }

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

    private ProductCategoryResponse mapToResponse(ProductCategory category) {
        return ProductCategoryResponse.builder()
                .categoryId(category.getCategoryId())
                .categoryName(category.getCategoryName())
                .skuPrefix(category.getSkuPrefix())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .createdBy(category.getCreatedBy())
                .updatedBy(category.getUpdatedBy())
                .build();
    }
}

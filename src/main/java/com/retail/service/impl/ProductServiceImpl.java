package com.retail.service.impl;

import com.retail.entity.*;
import com.retail.exception.ValidationException;
import com.retail.repository.ProductCategoryRepository;
import com.retail.repository.ProductRepository;
import com.retail.repository.SupplierRepository;
import com.retail.service.ProductService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;

    @Override
    @Transactional
    public Product createProduct(String productName, Integer categoryId, BigDecimal standardPrice, Integer supplierId) {
        if (productName == null || productName.trim().isEmpty()) {
            throw new ValidationException("Tên sản phẩm không được để trống");
        }
        if (standardPrice == null || standardPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Giá bán tiêu chuẩn phải lớn hơn hoặc bằng 0");
        }

        ProductCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ValidationException("Ngành hàng không tồn tại"));

        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ValidationException("Nhà cung cấp không tồn tại"));

        // Generate Sku: [Prefix]-[4 số tự tăng] (e.g. NC-0001)
        String prefix = category.getSkuPrefix();
        String maxSku = productRepository.findMaxSkuByPrefix(prefix);
        int seq = 1;
        if (maxSku != null && maxSku.startsWith(prefix + "-")) {
            try {
                String seqPart = maxSku.substring(prefix.length() + 1);
                seq = Integer.parseInt(seqPart) + 1;
            } catch (NumberFormatException ignored) {}
        }
        String sku = prefix + "-" + String.format("%04d", seq);

        Product product = Product.builder()
                .sku(sku)
                .productName(productName.trim())
                .category(category)
                .standardPrice(standardPrice)
                .defaultSupplier(supplier)
                .status(ProductStatus.Active)
                .build();

        return productRepository.save(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Product> list(String search, Integer categoryId, Integer supplierId, ProductStatus status, Pageable pageable) {
        return productRepository.searchProducts(search, categoryId, supplierId, status, pageable);
    }
}

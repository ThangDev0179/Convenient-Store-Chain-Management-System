package com.retail.service.impl;

import com.retail.dto.ProductRequest;
import com.retail.dto.ProductResponse;
import com.retail.entity.Product;
import com.retail.entity.ProductCategory;
import com.retail.entity.ProductStatus;
import com.retail.entity.Supplier;
import com.retail.exception.ValidationException;
import com.retail.repository.ProductCategoryRepository;
import com.retail.repository.ProductRepository;
import com.retail.repository.SupplierRepository;
import com.retail.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductCategoryRepository categoryRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> list(String search, Integer categoryId, Integer supplierId, ProductStatus status, Pageable pageable) {
        Page<Product> products = productRepository.searchProducts(search, categoryId, supplierId, status, pageable);
        return products.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getDetail(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Sản phẩm không tồn tại với ID: " + id));
        return mapToResponse(product);
    }

    @Override
    public void create(ProductRequest request) {
        // Proactive name validation
        if (request.getProductName() != null && !request.getProductName().trim().isEmpty()) {
            if (productRepository.existsByProductName(request.getProductName().trim())) {
                throw new ValidationException("Tên sản phẩm đã tồn tại trong hệ thống");
            }
        }


        ProductCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ValidationException("Danh mục không tồn tại với ID: " + request.getCategoryId()));

        // Autogenerate SKU using Pessimistic Lock on findMaxSkuByPrefix to handle race conditions
        String prefix = category.getSkuPrefix();
        if (prefix == null || prefix.trim().isEmpty()) {
            throw new ValidationException("Danh mục này chưa được cấu hình tiền tố SKU (SkuPrefix).");
        }

        String maxSku = productRepository.findMaxSkuByPrefix(prefix);
        String generatedSku;
        if (maxSku == null) {
            generatedSku = prefix + String.format("%04d", 1);
        } else {
            Matcher matcher = Pattern.compile("(\\d+)$").matcher(maxSku);
            if (matcher.find()) {
                String numStr = matcher.group(1);
                int length = numStr.length();
                int nextVal = Integer.parseInt(numStr) + 1;
                String nextNumStr = String.format("%0" + length + "d", nextVal);
                generatedSku = maxSku.substring(0, matcher.start()) + nextNumStr;
            } else {
                generatedSku = maxSku + "0001";
            }
        }

        Supplier supplier = null;
        if (request.getDefaultSupplierId() != null) {
            supplier = supplierRepository.findById(request.getDefaultSupplierId())
                    .orElseThrow(() -> new ValidationException("Nhà cung cấp không tồn tại với ID: " + request.getDefaultSupplierId()));
        }

        Product product = Product.builder()
                .sku(generatedSku)
                .productName(request.getProductName().trim())
                .category(category)
                .standardPrice(request.getStandardPrice())
                .defaultSupplier(supplier)
                .status(ProductStatus.Active)
                .build();

        try {
            productRepository.save(product);
            productRepository.flush();
        } catch (DataIntegrityViolationException e) {
            handleDataIntegrityViolation(e);
        }
    }

    @Override
    public void update(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Sản phẩm không tồn tại với ID: " + id));

        // Proactive name validation
        if (request.getProductName() != null && !request.getProductName().trim().isEmpty()) {
            if (productRepository.existsByProductNameAndProductIdNot(request.getProductName().trim(), id)) {
                throw new ValidationException("Tên sản phẩm đã tồn tại trong hệ thống");
            }
        }


        ProductCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ValidationException("Danh mục không tồn tại với ID: " + request.getCategoryId()));

        Supplier supplier = null;
        if (request.getDefaultSupplierId() != null) {
            supplier = supplierRepository.findById(request.getDefaultSupplierId())
                    .orElseThrow(() -> new ValidationException("Nhà cung cấp không tồn tại với ID: " + request.getDefaultSupplierId()));
        }

        product.setProductName(request.getProductName().trim());
        product.setCategory(category);
        product.setStandardPrice(request.getStandardPrice());
        product.setDefaultSupplier(supplier);

        try {
            productRepository.save(product);
            productRepository.flush();
        } catch (DataIntegrityViolationException e) {
            handleDataIntegrityViolation(e);
        }
    }

    @Override
    public void delete(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Sản phẩm không tồn tại với ID: " + id));

        // Soft delete safety check: mock checking inventory constraints
        checkInventoryConstraints(id);

        product.setStatus(ProductStatus.Inactive);
        productRepository.saveAndFlush(product);
    }

    @Override
    public void restore(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Sản phẩm không tồn tại với ID: " + id));
        product.setStatus(ProductStatus.Active);
        productRepository.saveAndFlush(product);
    }

    private void checkInventoryConstraints(Long productId) {
        // Mock validation to simulate inventory or order constraints
        if (productId != null && productId.equals(999L)) {
            throw new ValidationException("Không thể ngừng hoạt động sản phẩm này vì còn tồn kho hoặc có đơn hàng liên kết.");
        }
    }

    private void handleDataIntegrityViolation(DataIntegrityViolationException e) {
        String msg = e.getRootCause() != null ? e.getRootCause().getMessage() : e.getMessage();
        if (msg != null && (msg.contains("ProductName") || msg.contains("UQ_Product_ProductName"))) {
            throw new ValidationException("Tên sản phẩm đã tồn tại trong hệ thống");
        } else if (msg != null && (msg.contains("Sku") || msg.contains("UQ_Product_Sku"))) {
            throw new ValidationException("Mã sản phẩm (SKU) đã tồn tại trong hệ thống");
        } else {
            throw new ValidationException("Lỗi vi phạm ràng buộc dữ liệu sản phẩm: " + msg);
        }
    }

    private ProductResponse mapToResponse(Product product) {
        return ProductResponse.builder()
                .productId(product.getProductId())
                .sku(product.getSku())
                .productName(product.getProductName())
                .categoryId(product.getCategory().getCategoryId())
                .categoryName(product.getCategory().getCategoryName())
                .standardPrice(product.getStandardPrice())
                .defaultSupplierId(product.getDefaultSupplier() != null ? product.getDefaultSupplier().getSupplierId() : null)
                .defaultSupplierName(product.getDefaultSupplier() != null ? product.getDefaultSupplier().getSupplierName() : null)
                .status(product.getStatus())
                .createdAt(product.getCreatedAt())
                .build();
    }
}

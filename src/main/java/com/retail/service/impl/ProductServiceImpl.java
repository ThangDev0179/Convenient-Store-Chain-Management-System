package com.retail.service.impl;

import com.retail.dto.ProductRequest;
import com.retail.dto.ProductResponse;
import com.retail.dto.UomRequest;
import com.retail.dto.UomResponse;
import com.retail.entity.*;
import com.retail.exception.ValidationException;
import com.retail.repository.ProductCategoryRepository;
import com.retail.repository.ProductRepository;
import com.retail.repository.ProductUOMRepository;
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

    @Autowired
    private ProductUOMRepository uomRepository;

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

        // Proactive barcode validation
        if (request.getBarcode() != null && !request.getBarcode().trim().isEmpty()) {
            if (productRepository.existsByBarcode(request.getBarcode().trim())) {
                throw new ValidationException("Mã vạch đã tồn tại trong hệ thống");
            }
        }

        // Validate UOM barcodes
        validateUoms(null, request.getUoms());

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
            generatedSku = prefix + "-" + String.format("%04d", 1);
        } else {
            Matcher matcher = Pattern.compile("(\\d+)$").matcher(maxSku);
            if (matcher.find()) {
                String numStr = matcher.group(1);
                int length = numStr.length();
                int nextVal = Integer.parseInt(numStr) + 1;
                String nextNumStr = String.format("%0" + length + "d", nextVal);
                generatedSku = prefix + "-" + nextNumStr;
            } else {
                generatedSku = prefix + "-0001";
            }
        }

        Supplier supplier = null;
        if (request.getDefaultSupplierId() != null) {
            supplier = supplierRepository.findById(request.getDefaultSupplierId())
                    .orElseThrow(() -> new ValidationException("Nhà cung cấp không tồn tại với ID: " + request.getDefaultSupplierId()));
        }

        Product product = Product.builder()
                .sku(generatedSku)
                .barcode(request.getBarcode() != null ? request.getBarcode().trim() : null)
                .productName(request.getProductName().trim())
                .description(request.getDescription())
                .category(category)
                .standardPrice(request.getStandardPrice())
                .defaultSupplier(supplier)
                .status(ProductStatus.Active)
                .build();

        // Add UOMs
        if (request.getUoms() != null) {
            for (UomRequest u : request.getUoms()) {
                ProductUOM uom = ProductUOM.builder()
                        .uomName(u.getUomName().trim())
                        .isBaseUnit(u.getIsBaseUnit() != null && u.getIsBaseUnit())
                        .conversionRate(u.getConversionRate())
                        .barcode(u.getBarcode() != null ? u.getBarcode().trim() : null)
                        .standardPrice(u.getStandardPrice())
                        .status(u.getStatus() != null ? ProductUOMStatus.valueOf(u.getStatus().toUpperCase()) : ProductUOMStatus.ACTIVE)
                        .build();
                product.addUom(uom);
            }
        }

        try {
            productRepository.save(product);
            productRepository.flush();
        } catch (DataIntegrityViolationException e) {
            handleDataIntegrityViolation(e);
        }
    }

    @Override
    public void update(Long id, ProductRequest request) {
        System.out.println("--- BẮT ĐẦU CẬP NHẬT SẢN PHẨM ID: " + id + " ---");
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Sản phẩm không tồn tại với ID: " + id));

        // 1. Chuẩn hóa chuỗi an toàn để so sánh
        String currentName = product.getProductName() != null ? product.getProductName().trim() : "";
        String newName = request.getProductName() != null ? request.getProductName().trim() : "";

        String currentBarcode = product.getBarcode() != null ? product.getBarcode().trim() : "";
        String newBarcode = request.getBarcode() != null ? request.getBarcode().trim() : "";

        System.out.println("Tên hiện tại trong DB: [" + currentName + "]");
        System.out.println("Tên mới gửi từ Form: [" + newName + "]");

        // 2. CHỈ kiểm tra trùng lặp nếu người dùng THỰC SỰ đổi tên
        if (!newName.isEmpty() && !newName.equalsIgnoreCase(currentName)) {
            System.out.println("=> Tên bị thay đổi. Đang kiểm tra trùng lặp...");
            if (productRepository.existsByProductNameAndProductIdNot(newName, id)) {
                throw new ValidationException("Tên sản phẩm đã tồn tại trong hệ thống");
            }
        }

        // Tương tự với mã vạch
        if (!newBarcode.isEmpty() && !newBarcode.equalsIgnoreCase(currentBarcode)) {
            System.out.println("=> Mã vạch bị thay đổi. Đang kiểm tra trùng lặp...");
            if (productRepository.existsByBarcodeAndProductIdNot(newBarcode, id)) {
                throw new ValidationException("Mã vạch đã tồn tại trong hệ thống");
            }
        }

        // 3. Kiểm tra mã vạch UOM
        validateUoms(id, request.getUoms());

        ProductCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ValidationException("Danh mục không tồn tại với ID: " + request.getCategoryId()));

        Supplier supplier = null;
        if (request.getDefaultSupplierId() != null) {
            supplier = supplierRepository.findById(request.getDefaultSupplierId())
                    .orElseThrow(() -> new ValidationException("Nhà cung cấp không tồn tại với ID: " + request.getDefaultSupplierId()));
        }

        // Check category change to update SKU if needed
        if (!product.getCategory().getCategoryId().equals(category.getCategoryId())) {
            String newPrefix = category.getSkuPrefix();
            if (newPrefix != null && !newPrefix.trim().isEmpty()) {
                String maxSku = productRepository.findMaxSkuByPrefix(newPrefix);
                String generatedSku;
                if (maxSku == null) {
                    generatedSku = newPrefix + "-0001";
                } else {
                    Matcher matcher = Pattern.compile("(\\d+)$").matcher(maxSku);
                    if (matcher.find()) {
                        String numStr = matcher.group(1);
                        int nextVal = Integer.parseInt(numStr) + 1;
                        generatedSku = newPrefix + "-" + String.format("%0" + numStr.length() + "d", nextVal);
                    } else {
                        generatedSku = newPrefix + "-0001";
                    }
                }
                product.setSku(generatedSku);
            }
            product.setCategory(category);
        }

        // 4. CHỈ SET LẠI DỮ LIỆU NẾU CÓ THAY ĐỔI
        if (!currentName.equalsIgnoreCase(newName)) {
            product.setProductName(newName);
        }
        if (!currentBarcode.equalsIgnoreCase(newBarcode)) {
            product.setBarcode(newBarcode.isEmpty() ? null : newBarcode);
        }
        product.setDescription(request.getDescription());
        product.setStandardPrice(request.getStandardPrice());
        product.setDefaultSupplier(supplier);

        // 5. CẬP NHẬT UOM ĐÚNG CHUẨN (In-Place Update để bảo toàn ID và không vi phạm Khóa ngoại)
        if (request.getUoms() != null) {
            java.util.Set<Long> updatedUomIds = new java.util.HashSet<>();
            for (UomRequest u : request.getUoms()) {
                if (u.getId() != null) {
                    updatedUomIds.add(u.getId());
                    ProductUOM existingUom = product.getUoms().stream()
                            .filter(existing -> existing.getUomId().equals(u.getId()))
                            .findFirst().orElse(null);
                    if (existingUom != null) {
                        existingUom.setUomName(u.getUomName() != null ? u.getUomName().trim() : null);
                        existingUom.setIsBaseUnit(u.getIsBaseUnit() != null && u.getIsBaseUnit());
                        existingUom.setConversionRate(Boolean.TRUE.equals(u.getIsBaseUnit()) ? 1 : u.getConversionRate());
                        existingUom.setBarcode(u.getBarcode() != null ? u.getBarcode().trim() : null);
                        existingUom.setStandardPrice(Boolean.TRUE.equals(u.getIsBaseUnit()) ? request.getStandardPrice() : u.getStandardPrice());
                        if (u.getStatus() != null) {
                            existingUom.setStatus(ProductUOMStatus.valueOf(u.getStatus().toUpperCase()));
                        }
                    }
                } else {
                    ProductUOM newUom = ProductUOM.builder()
                            .uomName(u.getUomName() != null ? u.getUomName().trim() : null)
                            .isBaseUnit(u.getIsBaseUnit() != null && u.getIsBaseUnit())
                            .conversionRate(Boolean.TRUE.equals(u.getIsBaseUnit()) ? 1 : u.getConversionRate())
                            .barcode(u.getBarcode() != null ? u.getBarcode().trim() : null)
                            .standardPrice(Boolean.TRUE.equals(u.getIsBaseUnit()) ? request.getStandardPrice() : u.getStandardPrice())
                            .status(u.getStatus() != null ? ProductUOMStatus.valueOf(u.getStatus().toUpperCase()) : ProductUOMStatus.ACTIVE)
                            .build();
                    product.addUom(newUom);
                }
            }
            product.getUoms().removeIf(existing -> existing.getUomId() != null && !updatedUomIds.contains(existing.getUomId()));
        }

        // 6. LƯU DỮ LIỆU VÀ BẮT LỖI
        try {
            System.out.println("=> Bắt đầu lưu (flush) xuống Database...");
            productRepository.save(product);
            productRepository.flush();
            System.out.println("=> CẬP NHẬT THÀNH CÔNG!");
        } catch (DataIntegrityViolationException e) {
            System.out.println("=> LỖI DATA INTEGRITY: " + e.getMessage());
            if (e.getRootCause() != null) {
                System.out.println("=> NGUYÊN NHÂN GỐC: " + e.getRootCause().getMessage());
            }
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

    @Override
    @Transactional(readOnly = true)
    public int calculateBaseQuantity(Long uomId, int transactionQuantity) {
        ProductUOM uom = uomRepository.findById(uomId)
                .orElseThrow(() -> new ValidationException("Đơn vị tính không tồn tại với ID: " + uomId));
        return transactionQuantity * uom.getConversionRate();
    }

    private void validateUoms(Long productId, java.util.List<UomRequest> uomRequests) {
        if (uomRequests == null || uomRequests.isEmpty()) {
            return;
        }

        // 1. Check duplicate barcodes in the request list
        java.util.List<String> requestBarcodes = uomRequests.stream()
                .map(UomRequest::getBarcode)
                .filter(java.util.Objects::nonNull)
                .map(String::trim)
                .filter(b -> !b.isEmpty())
                .toList();
        long uniqueBarcodes = requestBarcodes.stream().distinct().count();
        if (uniqueBarcodes < requestBarcodes.size()) {
            throw new ValidationException("Mã vạch đơn vị tính không được trùng lặp trong cùng một sản phẩm.");
        }

        // 2. Check duplicate barcodes across the entire database
        for (UomRequest u : uomRequests) {
            String barcode = u.getBarcode() != null ? u.getBarcode().trim() : "";
            if (barcode.isEmpty()) {
                continue;
            }
            boolean exists;
            if (u.getId() != null) {
                exists = uomRepository.existsByBarcodeAndUomIdNot(barcode, u.getId());
            } else {
                exists = uomRepository.existsByBarcode(barcode);
            }
            if (exists) {
                throw new ValidationException("Mã vạch đơn vị tính '" + barcode + "' đã tồn tại ở sản phẩm khác.");
            }
        }
    }

    private void checkInventoryConstraints(Long productId) {
        // Mock validation to simulate inventory or order constraints
        if (productId != null && productId.equals(999L)) {
            throw new ValidationException("Không thể ngừng hoạt động sản phẩm này vì còn tồn kho hoặc có đơn hàng liên kết.");
        }
    }

    private void handleDataIntegrityViolation(DataIntegrityViolationException e) {
        String msg = e.getRootCause() != null ? e.getRootCause().getMessage() : e.getMessage();
        if (msg != null && (msg.contains("Barcode") || msg.contains("UQ_Product_Barcode"))) {
            throw new ValidationException("Mã vạch đã tồn tại trong hệ thống");
        } else if (msg != null && (msg.contains("ProductName") || msg.contains("UQ_Product_ProductName"))) {
            throw new ValidationException("Tên sản phẩm đã tồn tại trong hệ thống");
        } else if (msg != null && (msg.contains("Sku") || msg.contains("UQ_Product_Sku"))) {
            throw new ValidationException("Mã sản phẩm (SKU) đã tồn tại trong hệ thống");
        } else {
            throw new ValidationException("Lỗi vi phạm ràng buộc dữ liệu sản phẩm: " + msg);
        }
    }

    private ProductResponse mapToResponse(Product product) {
        java.util.List<UomResponse> uomResponses = new java.util.ArrayList<>();
        if (product.getUoms() != null) {
            for (ProductUOM uom : product.getUoms()) {
                uomResponses.add(UomResponse.builder()
                        .id(uom.getUomId())
                        .uomName(uom.getUomName())
                        .isBaseUnit(uom.getIsBaseUnit())
                        .conversionRate(uom.getConversionRate())
                        .barcode(uom.getBarcode())
                        .standardPrice(uom.getStandardPrice())
                        .status(uom.getStatus().name())
                        .build());
            }
        }

        return ProductResponse.builder()
                .productId(product.getProductId())
                .sku(product.getSku())
                .barcode(product.getBarcode())
                .productName(product.getProductName())
                .description(product.getDescription())
                .categoryId(product.getCategory().getCategoryId())
                .categoryName(product.getCategory().getCategoryName())
                .standardPrice(product.getStandardPrice())
                .defaultSupplierId(product.getDefaultSupplier() != null ? product.getDefaultSupplier().getSupplierId() : null)
                .defaultSupplierName(product.getDefaultSupplier() != null ? product.getDefaultSupplier().getSupplierName() : null)
                .status(product.getStatus())
                .uoms(uomResponses)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .createdBy(product.getCreatedBy())
                .updatedBy(product.getUpdatedBy())
                .build();
    }
}

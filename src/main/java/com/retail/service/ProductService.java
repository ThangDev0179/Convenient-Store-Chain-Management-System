package com.retail.service;

import com.retail.entity.Product;
import com.retail.entity.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {
    Product createProduct(String productName, Integer categoryId, java.math.BigDecimal standardPrice, Integer supplierId);
    Page<Product> list(String search, Integer categoryId, Integer supplierId, ProductStatus status, Pageable pageable);
}

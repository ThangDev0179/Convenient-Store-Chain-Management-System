package com.retail;

import com.retail.entity.*;
import com.retail.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductCategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @Test
    public void testAnonymousRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/admin/products"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testAdminCanAccessProducts() throws Exception {
        mockMvc.perform(get("/admin/products"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/product/list"))
                .andExpect(model().attributeExists("products"));
    }

    @Test
    @WithMockUser(username = "manager", roles = {"MANAGER"})
    public void testManagerBlockedFromProducts() throws Exception {
        mockMvc.perform(get("/admin/products"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/403"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testProductCrudFlow() throws Exception {
        // 1. Setup - Create Category
        ProductCategory category = ProductCategory.builder()
                .categoryName("JUnit Product Category")
                .skuPrefix("JUP")
                .build();
        category = categoryRepository.saveAndFlush(category);

        // 2. Create Product
        mockMvc.perform(post("/admin/products/create")
                        .param("productName", "JUnit Test Product")
                        .param("barcode", "8930000000001")
                        .param("categoryId", String.valueOf(category.getCategoryId()))
                        .param("standardPrice", "15000.00")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/products"));

        // Verify SKU generated with Prefix
        Product product = productRepository.findByBarcode("8930000000001").orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("JUP0001", product.getSku());

        // 3. Try to create duplicate barcode
        mockMvc.perform(post("/admin/products/create")
                        .param("productName", "Another Product")
                        .param("barcode", "8930000000001") // Duplicate barcode
                        .param("categoryId", String.valueOf(category.getCategoryId()))
                        .param("standardPrice", "20000.00")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/product/form"))
                .andExpect(model().hasErrors());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testDeleteProductSoftDelete() throws Exception {
        // Setup - Create Category and Product
        ProductCategory category = ProductCategory.builder()
                .categoryName("Soft Delete Category")
                .skuPrefix("SOFT")
                .build();
        category = categoryRepository.saveAndFlush(category);

        Product product = Product.builder()
                .sku("SOFT0001")
                .productName("Soft Delete Product")
                .category(category)
                .standardPrice(new BigDecimal("10000.00"))
                .status(ProductStatus.Active)
                .build();
        product = productRepository.saveAndFlush(product);

        // Perform delete
        mockMvc.perform(post("/admin/products/delete/{id}", product.getProductId())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/products"));

        // Verify status is Inactive
        entityManager.clear();
        Product deletedProduct = productRepository.findById(product.getProductId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(ProductStatus.Inactive, deletedProduct.getStatus());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testRestoreProduct() throws Exception {
        // Setup - Create Inactive Product
        ProductCategory category = ProductCategory.builder()
                .categoryName("Restore Category")
                .skuPrefix("RSTR")
                .build();
        category = categoryRepository.saveAndFlush(category);

        Product product = Product.builder()
                .sku("RSTR0001")
                .productName("Restore Product")
                .category(category)
                .standardPrice(new BigDecimal("10000.00"))
                .status(ProductStatus.Inactive)
                .build();
        product = productRepository.saveAndFlush(product);

        // Perform restore
        mockMvc.perform(post("/admin/products/restore/{id}", product.getProductId())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/products"));

        // Verify status is Active
        entityManager.clear();
        Product restoredProduct = productRepository.findById(product.getProductId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(ProductStatus.Active, restoredProduct.getStatus());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testDuplicateProductNameFails() throws Exception {
        ProductCategory category = ProductCategory.builder()
                .categoryName("Name Duplicate Cat")
                .skuPrefix("NDUP")
                .build();
        category = categoryRepository.saveAndFlush(category);

        Product product = Product.builder()
                .sku("NDUP0001")
                .productName("Duplicate Name Product")
                .category(category)
                .standardPrice(new BigDecimal("10000.00"))
                .status(ProductStatus.Active)
                .build();
        productRepository.saveAndFlush(product);

        // Try to create another product with the same name
        mockMvc.perform(post("/admin/products/create")
                        .param("productName", "Duplicate Name Product")
                        .param("categoryId", String.valueOf(category.getCategoryId()))
                        .param("standardPrice", "12000.00")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/product/form"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrorCode("product", "productName", "duplicate"));
    }
}

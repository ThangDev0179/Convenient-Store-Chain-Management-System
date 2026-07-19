package com.retail;

import com.retail.entity.*;
import com.retail.repository.*;
import com.retail.service.ProductService;
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
    private ProductUOMRepository uomRepository;

    @Autowired
    private ProductService productService;

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
    public void testManagerCanAccessProducts() throws Exception {
        mockMvc.perform(get("/admin/products"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/product/list"))
                .andExpect(model().attributeExists("products"));
    }

    @Test
    @WithMockUser(username = "staff", roles = {"STAFF"})
    public void testStaffBlockedFromProducts() throws Exception {
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

        // 2. Create Product with UOMs
        mockMvc.perform(post("/admin/products/create")
                        .param("productName", "JUnit Test Product")
                        .param("barcode", "8930000000001")
                        .param("categoryId", String.valueOf(category.getCategoryId()))
                        .param("standardPrice", "15000.00")
                        .param("uoms[0].uomName", "Lon")
                        .param("uoms[0].isBaseUnit", "true")
                        .param("uoms[0].conversionRate", "1")
                        .param("uoms[0].standardPrice", "15000.00")
                        .param("uoms[0].status", "ACTIVE")
                        .param("uoms[0].barcode", "UOM-BAR-001")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/products"));

        // Verify SKU generated with Prefix
        Product product = productRepository.findByBarcode("8930000000001").orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("JUP-0001", product.getSku());
        org.junit.jupiter.api.Assertions.assertEquals(1, product.getUoms().size());
        org.junit.jupiter.api.Assertions.assertEquals("Lon", product.getUoms().get(0).getUomName());

        // 3. Try to create duplicate barcode
        mockMvc.perform(post("/admin/products/create")
                        .param("productName", "Another Product")
                        .param("barcode", "8930000000001") // Duplicate barcode
                        .param("categoryId", String.valueOf(category.getCategoryId()))
                        .param("standardPrice", "20000.00")
                        .param("uoms[0].uomName", "Lon")
                        .param("uoms[0].isBaseUnit", "true")
                        .param("uoms[0].conversionRate", "1")
                        .param("uoms[0].standardPrice", "20000.00")
                        .param("uoms[0].status", "ACTIVE")
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
                .barcode("SOFT0001")
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
                .barcode("RSTR0001")
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
                .barcode("NDUP0001")
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
                        .param("uoms[0].uomName", "Lon")
                        .param("uoms[0].isBaseUnit", "true")
                        .param("uoms[0].conversionRate", "1")
                        .param("uoms[0].standardPrice", "12000.00")
                        .param("uoms[0].status", "ACTIVE")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/product/form"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrorCode("product", "productName", "duplicate"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testInvalidUomListFails() throws Exception {
        ProductCategory category = ProductCategory.builder()
                .categoryName("Uom Validation Cat")
                .skuPrefix("UVAL")
                .build();
        category = categoryRepository.saveAndFlush(category);

        // Test 1: No base unit
        mockMvc.perform(post("/admin/products/create")
                        .param("productName", "No Base Product")
                        .param("categoryId", String.valueOf(category.getCategoryId()))
                        .param("standardPrice", "10000")
                        .param("uoms[0].uomName", "Lon")
                        .param("uoms[0].isBaseUnit", "false")
                        .param("uoms[0].conversionRate", "1")
                        .param("uoms[0].standardPrice", "10000")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("product", "uoms"));

        // Test 2: Multiple base units
        mockMvc.perform(post("/admin/products/create")
                        .param("productName", "Multi Base Product")
                        .param("categoryId", String.valueOf(category.getCategoryId()))
                        .param("standardPrice", "10000")
                        .param("uoms[0].uomName", "Lon")
                        .param("uoms[0].isBaseUnit", "true")
                        .param("uoms[0].conversionRate", "1")
                        .param("uoms[0].standardPrice", "10000")
                        .param("uoms[1].uomName", "Thùng")
                        .param("uoms[1].isBaseUnit", "true")
                        .param("uoms[1].conversionRate", "1")
                        .param("uoms[1].standardPrice", "240000")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("product", "uoms"));

        // Test 3: Conversion unit rate <= 1
        mockMvc.perform(post("/admin/products/create")
                        .param("productName", "Invalid Rate Product")
                        .param("categoryId", String.valueOf(category.getCategoryId()))
                        .param("standardPrice", "10000")
                        .param("uoms[0].uomName", "Lon")
                        .param("uoms[0].isBaseUnit", "true")
                        .param("uoms[0].conversionRate", "1")
                        .param("uoms[0].standardPrice", "10000")
                        .param("uoms[1].uomName", "Thùng")
                        .param("uoms[1].isBaseUnit", "false")
                        .param("uoms[1].conversionRate", "1") // Should be > 1
                        .param("uoms[1].standardPrice", "200000")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("product", "uoms"));

        // Test 4: Duplicate names within list
        mockMvc.perform(post("/admin/products/create")
                        .param("productName", "Dup Names Product")
                        .param("categoryId", String.valueOf(category.getCategoryId()))
                        .param("standardPrice", "10000")
                        .param("uoms[0].uomName", "Lon")
                        .param("uoms[0].isBaseUnit", "true")
                        .param("uoms[0].conversionRate", "1")
                        .param("uoms[0].standardPrice", "10000")
                        .param("uoms[1].uomName", "Lon") // Duplicate name
                        .param("uoms[1].isBaseUnit", "false")
                        .param("uoms[1].conversionRate", "24")
                        .param("uoms[1].standardPrice", "240000")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("product", "uoms"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testDuplicateUomBarcodeAcrossDatabaseFails() throws Exception {
        ProductCategory category = categoryRepository.saveAndFlush(ProductCategory.builder()
                .categoryName("Barcode Cat")
                .skuPrefix("BCAT")
                .build());

        // Create first product with UOM Barcode directly via repository
        Product productOne = Product.builder()
                .sku("BCAT0001")
                .productName("Product One")
                .category(category)
                .standardPrice(new BigDecimal("10000"))
                .status(ProductStatus.Active)
                .build();

        ProductUOM uomOne = ProductUOM.builder()
                .uomName("Lon")
                .isBaseUnit(true)
                .conversionRate(1)
                .standardPrice(new BigDecimal("10000"))
                .barcode("DUP-UOM-BARCODE")
                .status(ProductUOMStatus.ACTIVE)
                .build();

        productOne.addUom(uomOne);
        productRepository.saveAndFlush(productOne);

        // Attempt to create second product with same UOM Barcode
        mockMvc.perform(post("/admin/products/create")
                        .param("productName", "Product Two")
                        .param("categoryId", String.valueOf(category.getCategoryId()))
                        .param("standardPrice", "12000")
                        .param("uoms[0].uomName", "Chai")
                        .param("uoms[0].isBaseUnit", "true")
                        .param("uoms[0].conversionRate", "1")
                        .param("uoms[0].standardPrice", "12000")
                        .param("uoms[0].barcode", "DUP-UOM-BARCODE") // Duplicate barcode
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("product", "uoms"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testCalculateBaseQuantity() {
        ProductCategory category = categoryRepository.saveAndFlush(ProductCategory.builder()
                .categoryName("Quantity Calc Category")
                .skuPrefix("QCALC")
                .build());

        Product product = Product.builder()
                .sku("QCALC0001")
                .productName("Calculated Product")
                .category(category)
                .standardPrice(new BigDecimal("10000"))
                .status(ProductStatus.Active)
                .build();

        ProductUOM uom = ProductUOM.builder()
                .uomName("Thùng")
                .isBaseUnit(false)
                .conversionRate(24)
                .standardPrice(new BigDecimal("240000"))
                .status(ProductUOMStatus.ACTIVE)
                .build();

        product.addUom(uom);
        productRepository.saveAndFlush(product);

        // Perform calculation
        int baseQuantity = productService.calculateBaseQuantity(uom.getUomId(), 10);
        org.junit.jupiter.api.Assertions.assertEquals(240, baseQuantity);
    }

    @Test
    public void printAllProducts() {
        productRepository.findAll().forEach(p -> {
            System.out.println("--- DIAGNOSTIC PRODUCT: ID=" + p.getProductId() + ", Name=[" + p.getProductName() + "], SKU=" + p.getSku() + ", Status=" + p.getStatus());
        });
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testUpdateProductSucceedsWithoutChangingName() throws Exception {
        ProductCategory category = categoryRepository.saveAndFlush(ProductCategory.builder()
                .categoryName("Update Name Cat")
                .skuPrefix("UCAT")
                .build());

        Product product = Product.builder()
                .sku("UCAT0001")
                .productName("Sữa tươi không đường")
                .category(category)
                .standardPrice(new BigDecimal("10000"))
                .status(ProductStatus.Active)
                .build();

        ProductUOM uom = ProductUOM.builder()
                .uomName("Lon")
                .isBaseUnit(true)
                .conversionRate(1)
                .standardPrice(new BigDecimal("10000"))
                .status(ProductUOMStatus.ACTIVE)
                .build();

        product.addUom(uom);
        product = productRepository.saveAndFlush(product);

        // Perform update sending same name
        mockMvc.perform(post("/admin/products/edit/{id}", product.getProductId())
                        .param("productName", "Sữa tươi không đường")
                        .param("categoryId", String.valueOf(category.getCategoryId()))
                        .param("standardPrice", "10000")
                        .param("uoms[0].id", String.valueOf(uom.getUomId()))
                        .param("uoms[0].uomName", "Lon")
                        .param("uoms[0].isBaseUnit", "true")
                        .param("uoms[0].conversionRate", "1")
                        .param("uoms[0].standardPrice", "10000")
                        .param("uoms[0].status", "ACTIVE")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/products"));
    }
}

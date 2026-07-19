package com.retail;

import com.retail.entity.Product;
import com.retail.entity.ProductCategory;
import com.retail.entity.ProductStatus;
import com.retail.repository.ProductCategoryRepository;
import com.retail.repository.ProductRepository;
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
public class ProductCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductCategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @Test
    public void testAnonymousRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/admin/categories"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testAdminCanAccessCategories() throws Exception {
        mockMvc.perform(get("/admin/categories"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/category/list"))
                .andExpect(model().attributeExists("categories"));
    }

    @Test
    @WithMockUser(username = "manager", roles = {"MANAGER"})
    public void testManagerBlockedFromCategories() throws Exception {
        mockMvc.perform(get("/admin/categories"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/403"));
    }

    @Test
    @WithMockUser(username = "staff", roles = {"STAFF"})
    public void testStaffBlockedFromCategories() throws Exception {
        mockMvc.perform(get("/admin/categories"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/403"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testCategoryCrudFlow() throws Exception {
        // 1. Create Category
        mockMvc.perform(post("/admin/categories/create")
                        .param("categoryName", "JUnit Test Category")
                        .param("skuPrefix", "JUT")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/categories"));

        // 2. Try to create duplicate (should return to form with error)
        mockMvc.perform(post("/admin/categories/create")
                        .param("categoryName", "Another Category")
                        .param("skuPrefix", "JUT")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/category/form"))
                .andExpect(model().hasErrors());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testGetCategoryDetail() throws Exception {
        // Setup - Create a category
        ProductCategory category = ProductCategory.builder()
                .categoryName("Detail Test Category")
                .skuPrefix("DETL")
                .build();
        category = categoryRepository.saveAndFlush(category);

        mockMvc.perform(get("/admin/categories/detail/{id}", category.getCategoryId()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/category/detail"))
                .andExpect(model().attributeExists("category"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testDeleteCategoryWithProductFails() throws Exception {
        // 1. Setup - Create a category
        ProductCategory category = ProductCategory.builder()
                .categoryName("Linked Category")
                .skuPrefix("LINKD")
                .build();
        category = categoryRepository.saveAndFlush(category);

        // 2. Setup - Create a product linked to this category
        Product product = Product.builder()
                .sku("LINKD-0001")
                .productName("Linked Test Product")
                .category(category)
                .standardPrice(new BigDecimal("10000.00"))
                .status(ProductStatus.Active)
                .build();
        productRepository.saveAndFlush(product);

        // Clear persistence context to decouple entity instances from session cache
        entityManager.clear();

        // 3. Attempt Delete Category (should catch integrity error and set flash attribute error)
        mockMvc.perform(post("/admin/categories/delete/{id}", category.getCategoryId())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/categories"))
                .andExpect(flash().attribute("error", "Không thể xóa danh mục này vì đang có sản phẩm liên kết"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testDuplicateCategoryNameFails() throws Exception {
        // Setup - Create a category
        ProductCategory category = ProductCategory.builder()
                .categoryName("Name Duplicate Category")
                .skuPrefix("NDUP")
                .build();
        categoryRepository.saveAndFlush(category);

        // Try to create another category with the same name
        mockMvc.perform(post("/admin/categories/create")
                        .param("categoryName", "Name Duplicate Category")
                        .param("skuPrefix", "DIFF")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/category/form"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrorCode("category", "categoryName", "duplicate"));
    }
}

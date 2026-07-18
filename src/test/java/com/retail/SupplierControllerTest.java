package com.retail;

import com.retail.dto.SupplierRequest;
import com.retail.entity.SupplierStatus;
import com.retail.service.SupplierService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class SupplierControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SupplierService supplierService;

    @Test
    public void testAnonymousRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/admin/suppliers"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testAdminCanAccessSuppliers() throws Exception {
        mockMvc.perform(get("/admin/suppliers"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/supplier/list"))
                .andExpect(model().attributeExists("suppliers"));
    }

    @Test
    @WithMockUser(username = "manager", roles = {"MANAGER"})
    public void testManagerBlockedFromSuppliers() throws Exception {
        mockMvc.perform(get("/admin/suppliers"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/403"));
    }

    @Test
    @WithMockUser(username = "staff", roles = {"STAFF"})
    public void testStaffBlockedFromSuppliers() throws Exception {
        mockMvc.perform(get("/admin/suppliers"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/403"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testSupplierCrudFlow() throws Exception {
        // 1. Create Supplier
        mockMvc.perform(post("/admin/suppliers/create")
                        .param("supplierName", "Test JUnit Supplier")
                        .param("contactPhone", "0912345678")
                        .param("contactEmail", "junit@supplier.com")
                        .param("address", "123 JUnit Rd")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/suppliers"));

        // 2. Try to create duplicate (should return to form with error)
        mockMvc.perform(post("/admin/suppliers/create")
                        .param("supplierName", "Another Supplier")
                        .param("contactPhone", "0912345678") // duplicate phone
                        .param("contactEmail", "another@supplier.com")
                        .param("address", "123 Main St")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/supplier/form"))
                .andExpect(model().hasErrors());
    }
}

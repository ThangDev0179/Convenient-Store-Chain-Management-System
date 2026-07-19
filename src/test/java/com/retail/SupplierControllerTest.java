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

    @Autowired
    private com.retail.repository.SupplierRepository supplierRepository;

    @Autowired
    private com.retail.repository.PurchaseOrderRepository purchaseOrderRepository;

    @Autowired
    private com.retail.repository.BranchRepository branchRepository;

    @Autowired
    private com.retail.repository.EmployeeRepository employeeRepository;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

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

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testGetSupplierDetail() throws Exception {
        // Setup - Create a supplier
        com.retail.entity.Supplier supplier = com.retail.entity.Supplier.builder()
                .supplierName("Detail Supplier Test")
                .contactPhone("0988776655")
                .contactEmail("detail@supplier.com")
                .status(com.retail.entity.SupplierStatus.Active)
                .build();
        supplier = supplierRepository.saveAndFlush(supplier);

        mockMvc.perform(get("/admin/suppliers/detail/{id}", supplier.getSupplierId()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/supplier/detail"))
                .andExpect(model().attributeExists("supplier"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testDeleteSupplierWithPendingOrderFails() throws Exception {
        // 1. Setup - Create a supplier
        com.retail.entity.Supplier supplier = com.retail.entity.Supplier.builder()
                .supplierName("Pending Supplier Test")
                .contactPhone("0977665544")
                .contactEmail("pending@supplier.com")
                .status(com.retail.entity.SupplierStatus.Active)
                .build();
        supplier = supplierRepository.saveAndFlush(supplier);

        // 2. Fetch seeded Branch and Employee
        com.retail.entity.Branch branch = branchRepository.findAll().get(0);
        com.retail.entity.Employee employee = employeeRepository.findAll().get(0);

        // 3. Create a pending purchase order
        com.retail.entity.PurchaseOrder po = com.retail.entity.PurchaseOrder.builder()
                .poCode("PO-TEST-0001")
                .branch(branch)
                .supplier(supplier)
                .status(com.retail.entity.PurchaseOrderStatus.Draft)
                .createdBy(employee)
                .build();
        purchaseOrderRepository.saveAndFlush(po);

        // Clear context
        entityManager.clear();

        // 4. Try to delete (soft delete/inactive) the supplier
        mockMvc.perform(post("/admin/suppliers/delete/{id}", supplier.getSupplierId())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/suppliers"))
                .andExpect(flash().attribute("error", "Không thể ngừng hoạt động nhà cung cấp này vì đang có đơn mua hàng (Purchase Order) chưa hoàn tất."));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testDuplicateSupplierNameFails() throws Exception {
        // Setup - Create a supplier
        com.retail.entity.Supplier supplier = com.retail.entity.Supplier.builder()
                .supplierName("Duplicate Supplier Name Test")
                .contactPhone("0988111222")
                .contactEmail("dup@supplier.com")
                .status(com.retail.entity.SupplierStatus.Active)
                .build();
        supplierRepository.saveAndFlush(supplier);

        // Try to create another supplier with the same name
        mockMvc.perform(post("/admin/suppliers/create")
                        .param("supplierName", "Duplicate Supplier Name Test")
                        .param("contactPhone", "0988333444") // Different phone
                        .param("contactEmail", "diff@supplier.com") // Different email
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/supplier/form"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrorCode("supplier", "supplierName", "duplicate"));
    }
}

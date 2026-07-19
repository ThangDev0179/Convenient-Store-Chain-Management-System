package com.retail;

import com.retail.entity.Branch;
import com.retail.repository.BranchRepository;
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
public class BranchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BranchRepository branchRepository;

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testGetBranchList() throws Exception {
        mockMvc.perform(get("/admin/branches"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/branches/branch-list"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testGetNewBranchForm() throws Exception {
        mockMvc.perform(get("/admin/branches/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/branches/branch-form"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testCreateBranchFlow() throws Exception {
        mockMvc.perform(post("/admin/branches/new")
                        .param("branchCode", "TESTBR01")
                        .param("branchName", "Test Branch Name")
                        .param("address", "123 Test Street")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/branches"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testGetEditBranchForm() throws Exception {
        Branch branch = branchRepository.findAll().stream().findFirst().orElseThrow();
        mockMvc.perform(get("/admin/branches/{id}/edit", branch.getBranchId()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/branches/branch-form"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testUpdateBranchFlow() throws Exception {
        Branch branch = branchRepository.findAll().stream().findFirst().orElseThrow();
        mockMvc.perform(post("/admin/branches/{id}/edit", branch.getBranchId())
                        .param("branchName", "Updated Branch Name")
                        .param("address", "Updated Address")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/branches"));
    }
}

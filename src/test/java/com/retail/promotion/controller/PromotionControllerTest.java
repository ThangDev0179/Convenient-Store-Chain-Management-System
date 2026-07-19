package com.retail.promotion.controller;

import com.retail.dto.PromotionDetailResponse;
import com.retail.dto.PromotionResponse;
import com.retail.entity.PromotionStatus;
import com.retail.service.PromotionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class PromotionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PromotionService promotionService;

    @MockBean
    private com.retail.service.ProductService productService;

    @Test
    public void testAnonymousRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/admin/promotions"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(username = "staff", roles = {"STAFF"})
    public void testStaffAccessForbidden() throws Exception {
        mockMvc.perform(get("/admin/promotions"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/403"));
    }

    @Test
    @WithMockUser(username = "manager", roles = {"MANAGER"})
    public void testManagerAccessForbidden() throws Exception {
        mockMvc.perform(get("/admin/promotions"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/403"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testAdminAccessAllowed() throws Exception {
        given(promotionService.list(any(), any(), any())).willReturn(org.springframework.data.domain.Page.empty());

        mockMvc.perform(get("/admin/promotions"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/promotion/list"))
                .andExpect(model().attributeExists("promotions"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void create_ShouldRedirectToListView_WhenInputIsValid() throws Exception {
        given(productService.list(any(), any(), any(), any(), any())).willReturn(org.springframework.data.domain.Page.empty());
        
        mockMvc.perform(post("/admin/promotions/create")
                        .param("promotionName", "Khuyến mãi tết")
                        .param("startDateTime", "2025-01-01T00:00")
                        .param("endDateTime", "2025-01-05T00:00")
                        .param("details[0].productId", "1")
                        .param("details[0].discountType", "Percentage")
                        .param("details[0].discountValue", "10.00")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/promotions"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void create_ShouldReturnFormWithErrors_WhenInputIsInvalid() throws Exception {
        given(productService.list(any(), any(), any(), any(), any())).willReturn(org.springframework.data.domain.Page.empty());

        // Empty promotionName or endDateTime before startDateTime
        mockMvc.perform(post("/admin/promotions/create")
                        .param("promotionName", "")
                        .param("startDateTime", "2025-01-05T00:00")
                        .param("endDateTime", "2025-01-01T00:00")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/promotion/form"))
                .andExpect(model().attributeExists("promotion"));
    }
}

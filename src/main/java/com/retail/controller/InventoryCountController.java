package com.retail.controller;

import com.retail.dto.InventoryCountRequest;
import com.retail.entity.InventoryCount;
import com.retail.repository.BranchRepository;
import com.retail.repository.ProductRepository;
import com.retail.security.CustomUserDetails;
import com.retail.service.InventoryCountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/inventory-count")
@RequiredArgsConstructor
public class InventoryCountController {

    private final InventoryCountService countService;
    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;

    private Long getEmployeeId(Authentication auth) {
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        return userDetails.getEmployee().getEmployeeId();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    public String listCounts(Model model) {
        List<InventoryCount> counts = countService.getAllCounts();
        model.addAttribute("counts", counts);
        return "inventorycount/list";
    }

    @GetMapping("/create")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    public String showCreateForm(Model model) {
        model.addAttribute("request", new InventoryCountRequest());
        model.addAttribute("branches", branchRepository.findAll());
        model.addAttribute("products", productRepository.findAll());
        return "inventorycount/create";
    }

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    public String createDraftCount(@Valid @ModelAttribute("request") InventoryCountRequest request,
                                   BindingResult result,
                                   Authentication auth,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("branches", branchRepository.findAll());
            model.addAttribute("products", productRepository.findAll());
            return "inventorycount/create";
        }
        try {
            countService.createDraftCount(request, getEmployeeId(auth));
            redirectAttributes.addFlashAttribute("successMessage", "Đã tạo phiếu kiểm kê nháp thành công.");
            return "redirect:/inventory-count";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/inventory-count/create";
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    public String viewCount(@PathVariable Long id, Model model) {
        InventoryCount count = countService.getCountById(id);
        model.addAttribute("count", count);
        return "inventorycount/detail";
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    public String submitCount(@PathVariable Long id, Authentication auth, RedirectAttributes redirectAttributes) {
        try {
            countService.submitCount(id, getEmployeeId(auth));
            redirectAttributes.addFlashAttribute("successMessage", "Đã gửi phiếu kiểm kê để duyệt.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/inventory-count/" + id;
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public String approveCount(@PathVariable Long id, Authentication auth, RedirectAttributes redirectAttributes) {
        try {
            countService.approveCount(id, getEmployeeId(auth));
            redirectAttributes.addFlashAttribute("successMessage", "Đã duyệt phiếu và cập nhật tồn kho thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/inventory-count/" + id;
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public String rejectCount(@PathVariable Long id, Authentication auth, RedirectAttributes redirectAttributes) {
        try {
            countService.rejectCount(id, getEmployeeId(auth));
            redirectAttributes.addFlashAttribute("successMessage", "Đã từ chối phiếu kiểm kê.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/inventory-count/" + id;
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    public String cancelCount(@PathVariable Long id, Authentication auth, RedirectAttributes redirectAttributes) {
        try {
            countService.cancelCount(id, getEmployeeId(auth));
            redirectAttributes.addFlashAttribute("successMessage", "Đã hủy bỏ bản nháp phiếu kiểm kê.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/inventory-count";
    }
}
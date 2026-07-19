package com.retail.controller;

import com.retail.entity.BranchPriceRequest;
import com.retail.entity.Product;
import com.retail.repository.ProductRepository;
import com.retail.security.CustomUserDetails;
import com.retail.service.BranchPriceService;
import com.retail.exception.ValidationException;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/manager/prices")
@RequiredArgsConstructor
public class BranchPriceController {

    private final BranchPriceService branchPriceService;
    private final ProductRepository productRepository;

    @GetMapping
    public String showPriceOverridePage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {

        Integer branchId = userDetails.getEmployee().getBranch() != null ?
                userDetails.getEmployee().getBranch().getBranchId() : null;

        if (branchId == null) {
            model.addAttribute("error", "Nhân viên chưa được gán vào chi nhánh làm việc nào");
            return "error/403";
        }

        List<BranchPriceRequest> requests = branchPriceService.getRequestsByBranch(branchId);
        List<Product> products = productRepository.findAll();

        model.addAttribute("requests", requests);
        model.addAttribute("products", products);
        model.addAttribute("branchId", branchId);
        return "manager/price-request";
    }

    @PostMapping("/new")
    public String submitPriceProposal(
            @RequestParam("productId") Long productId,
            @RequestParam("proposedPrice") BigDecimal proposedPrice,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        try {
            Integer branchId = userDetails.getEmployee().getBranch().getBranchId();
            branchPriceService.createRequest(branchId, productId, proposedPrice, userDetails.getEmployee().getEmployeeId());
            redirectAttributes.addFlashAttribute("success", "Đã gửi đề xuất thay đổi giá bán chi nhánh thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/manager/prices";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/approve")
    public String approveProposal(
            @PathVariable("id") Long requestId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        try {
            branchPriceService.approveRequest(requestId, userDetails.getEmployee().getEmployeeId());
            redirectAttributes.addFlashAttribute("success", "Đã duyệt và áp dụng giá bán chi nhánh mới thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/manager/prices";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/reject")
    public String rejectProposal(
            @PathVariable("id") Long requestId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        try {
            branchPriceService.rejectRequest(requestId, userDetails.getEmployee().getEmployeeId());
            redirectAttributes.addFlashAttribute("success", "Từ chối đề xuất thay đổi giá bán chi nhánh thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/manager/prices";
    }
}

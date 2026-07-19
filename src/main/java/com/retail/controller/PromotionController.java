package com.retail.controller;

import com.retail.entity.Promotion;
import com.retail.entity.Product;
import com.retail.repository.ProductRepository;
import com.retail.security.CustomUserDetails;
import com.retail.service.PromotionService;
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
@RequestMapping("/admin/promotions")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;
    private final ProductRepository productRepository;

    @GetMapping
    public String showPromotionsPage(Model model) {
        List<Promotion> promotions = promotionService.getAllPromotions();
        List<Product> products = productRepository.findAll();
        model.addAttribute("promotions", promotions);
        model.addAttribute("products", products);
        return "admin/promotion/promotion-list";
    }

    @PostMapping("/new")
    public String createPromotion(
            @RequestParam("promotionName") String name,
            @RequestParam("startDateTime") String start,
            @RequestParam("endDateTime") String end,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        try {
            // Nhập dạng YYYY-MM-DD HH:MM:SS
            String startStr = start.replace("T", " ") + ":00";
            String endStr = end.replace("T", " ") + ":00";
            promotionService.createPromotion(name, startStr, endStr, userDetails.getEmployee().getEmployeeId());
            redirectAttributes.addFlashAttribute("success", "Đã tạo chương trình khuyến mãi thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/promotions";
    }

    @PostMapping("/{id}/add-detail")
    public String addDetail(
            @PathVariable("id") Long promotionId,
            @RequestParam("productId") Long productId,
            @RequestParam("discountType") String discountType,
            @RequestParam("discountValue") BigDecimal discountValue,
            RedirectAttributes redirectAttributes) {
        try {
            promotionService.addDetail(promotionId, productId, discountType, discountValue);
            redirectAttributes.addFlashAttribute("success", "Thêm sản phẩm vào khuyến mãi thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/promotions";
    }

    @PostMapping("/{id}/activate")
    public String activate(
            @PathVariable("id") Long promotionId,
            RedirectAttributes redirectAttributes) {
        try {
            promotionService.activatePromotion(promotionId);
            redirectAttributes.addFlashAttribute("success", "Đã kích hoạt chương trình khuyến mãi!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/promotions";
    }

    @PostMapping("/{id}/cancel")
    public String cancel(
            @PathVariable("id") Long promotionId,
            RedirectAttributes redirectAttributes) {
        try {
            promotionService.cancelPromotion(promotionId);
            redirectAttributes.addFlashAttribute("success", "Đã hủy chương trình khuyến mãi!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/promotions";
    }
}

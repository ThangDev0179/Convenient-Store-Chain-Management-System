package com.retail.controller;

import com.retail.dto.PromotionDetailRequest;
import com.retail.dto.PromotionRequest;
import com.retail.dto.PromotionResponse;
import com.retail.entity.PromotionStatus;
import com.retail.exception.ValidationException;
import com.retail.service.PromotionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;

@Controller
@RequestMapping("/admin/promotions")
@PreAuthorize("hasRole('ADMIN')")
public class PromotionController {

    @Autowired
    private PromotionService promotionService;

    @Autowired
    private com.retail.service.ProductService productService;

    // ─── DANH SÁCH ───────────────────────────────────────────────────────────────
    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String status,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "10") int size,
                       Model model) {

        PromotionStatus statusEnum = null;
        if (status != null && !status.isBlank()) {
            try { statusEnum = PromotionStatus.valueOf(status); } catch (Exception ignored) {}
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<PromotionResponse> promotionPage = promotionService.list(keyword, statusEnum, pageable);

        model.addAttribute("promotions",  promotionPage.getContent());
        model.addAttribute("totalPages",  promotionPage.getTotalPages());
        model.addAttribute("totalItems",  promotionPage.getTotalElements());
        model.addAttribute("currentPage", page);
        model.addAttribute("size",        size);
        model.addAttribute("keyword",     keyword);
        model.addAttribute("status",      status);
        return "admin/promotion/list";
    }

    // ─── FORM TẠO MỚI ────────────────────────────────────────────────────────────
    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("promotion", PromotionRequest.builder().details(new ArrayList<>()).build());
        model.addAttribute("isEdit",    false);
        model.addAttribute("products",  getProductsForForm());
        return "admin/promotion/form";
    }

    // ─── XỬ LÝ TẠO MỚI ──────────────────────────────────────────────────────────
    @PostMapping("/create")
    public String create(@Valid @ModelAttribute("promotion") PromotionRequest request,
                         BindingResult bindingResult,
                         Authentication authentication,
                         Model model,
                         RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit",   false);
            model.addAttribute("products", getProductsForForm());
            return "admin/promotion/form";
        }

        try {
            promotionService.create(request, authentication.getName());
            redirectAttributes.addFlashAttribute("success", "Tạo chương trình khuyến mãi thành công!");
            return "redirect:/admin/promotions";
        } catch (ValidationException e) {
            mapException(e, bindingResult, model);
            model.addAttribute("isEdit",   false);
            model.addAttribute("products", getProductsForForm());
            return "admin/promotion/form";
        }
    }

    // ─── FORM CHỈNH SỬA ──────────────────────────────────────────────────────────
    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            PromotionResponse detail = promotionService.getDetail(id);

            PromotionRequest request = PromotionRequest.builder()
                    .promotionName(detail.getPromotionName())
                    .startDateTime(detail.getStartDateTime())
                    .endDateTime(detail.getEndDateTime())
                    .details(detail.getDetails() == null ? new ArrayList<>() :
                            detail.getDetails().stream().map(d -> PromotionDetailRequest.builder()
                                    .promotionDetailId(d.getPromotionDetailId())
                                    .productId(d.getProductId())
                                    .discountType(d.getDiscountType().name())
                                    .discountValue(d.getDiscountValue())
                                    .build()).toList())
                    .build();

            model.addAttribute("promotion",     request);
            model.addAttribute("isEdit",        true);
            model.addAttribute("promotionId",   id);
            model.addAttribute("currentStatus", detail.getStatus());
            model.addAttribute("products",      getProductsForForm());
            return "admin/promotion/form";
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/promotions";
        }
    }

    // ─── XỬ LÝ CẬP NHẬT ─────────────────────────────────────────────────────────
    @PostMapping("/edit/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("promotion") PromotionRequest request,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit",      true);
            model.addAttribute("promotionId", id);
            model.addAttribute("products",    getProductsForForm());
            return "admin/promotion/form";
        }

        try {
            promotionService.update(id, request);
            redirectAttributes.addFlashAttribute("success", "Cập nhật chương trình khuyến mãi thành công!");
            return "redirect:/admin/promotions";
        } catch (ValidationException e) {
            mapException(e, bindingResult, model);
            model.addAttribute("isEdit",      true);
            model.addAttribute("promotionId", id);
            model.addAttribute("products",    getProductsForForm());
            return "admin/promotion/form";
        }
    }

    // ─── CHI TIẾT ────────────────────────────────────────────────────────────────
    @GetMapping("/detail/{id}")
    public String detail(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("promotion", promotionService.getDetail(id));
            return "admin/promotion/detail";
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/promotions";
        }
    }

    // ─── KÍCH HOẠT ───────────────────────────────────────────────────────────────
    @PostMapping("/activate/{id}")
    public String activate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            promotionService.activate(id);
            redirectAttributes.addFlashAttribute("success", "Đã kích hoạt chương trình khuyến mãi!");
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/promotions";
    }

    // ─── HỦY (SOFT DELETE) ───────────────────────────────────────────────────────
    @PostMapping("/cancel/{id}")
    public String cancel(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            promotionService.cancel(id);
            redirectAttributes.addFlashAttribute("success", "Đã hủy chương trình khuyến mãi!");
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/promotions";
    }

    // ─── Helper ──────────────────────────────────────────────────────────────────
    private void mapException(ValidationException e, BindingResult br, Model model) {
        String msg = e.getMessage();
        if (msg.contains("Tên chương trình")) {
            br.rejectValue("promotionName", "duplicate", msg);
        } else if (msg.contains("Ngày kết thúc")) {
            br.rejectValue("endDateTime", "invalid", msg);
        } else if (msg.contains("Sản phẩm") || msg.contains("giảm giá")
                || msg.contains("phần trăm") || msg.contains("tiền mặt")) {
            model.addAttribute("detailError", msg);
        } else {
            model.addAttribute("error", msg);
        }
    }

    private java.util.List<ProductDto> getProductsForForm() {
        return productService.list(null, null, null, com.retail.entity.ProductStatus.Active, org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE))
                .getContent().stream()
                .map(p -> new ProductDto(p.getProductId(), p.getSku(), p.getProductName()))
                .toList();
    }

    public static record ProductDto(Long productId, String sku, String productName) {}
}

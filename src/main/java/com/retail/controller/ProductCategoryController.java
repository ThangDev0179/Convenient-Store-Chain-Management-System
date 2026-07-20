package com.retail.controller;

import com.retail.dto.ProductCategoryRequest;
import com.retail.dto.ProductCategoryResponse;
import com.retail.exception.ValidationException;
import com.retail.service.ProductCategoryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/categories")
public class ProductCategoryController {

    @Autowired
    private ProductCategoryService categoryService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    @GetMapping
    public String listCategories(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5") int size,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "categoryId"));
        Page<ProductCategoryResponse> categoryPage = categoryService.list(search, pageable);

        model.addAttribute("categories", categoryPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", categoryPage.getTotalPages());
        model.addAttribute("totalItems", categoryPage.getTotalElements());
        model.addAttribute("size", size);
        model.addAttribute("search", search);

        return "admin/category/list";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("category", new ProductCategoryRequest());
        model.addAttribute("isEdit", false);
        return "admin/category/form";
    }

    @PostMapping("/create")
    public String createCategory(
            @Valid @ModelAttribute("category") ProductCategoryRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", false);
            return "admin/category/form";
        }

        try {
            categoryService.create(request);
            redirectAttributes.addFlashAttribute("success", "Thêm mới danh mục ngành hàng thành công!");
            return "redirect:/admin/categories";
        } catch (ValidationException e) {
            if (e.getMessage().contains("SKU")) {
                bindingResult.rejectValue("skuPrefix", "duplicate", e.getMessage());
            } else if (e.getMessage().contains("danh mục") || e.getMessage().contains("Tên")) {
                bindingResult.rejectValue("categoryName", "duplicate", e.getMessage());
            } else {
                model.addAttribute("error", e.getMessage());
            }
            model.addAttribute("isEdit", false);
            return "admin/category/form";
        }
    }

    @GetMapping("/detail/{id}")
    public String showDetail(
            @PathVariable("id") Integer id,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            ProductCategoryResponse detail = categoryService.getDetail(id);
            model.addAttribute("category", detail);
            return "admin/category/detail";
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/categories";
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(
            @PathVariable("id") Integer id,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            ProductCategoryResponse detail = categoryService.getDetail(id);
            ProductCategoryRequest request = ProductCategoryRequest.builder()
                    .categoryName(detail.getCategoryName())
                    .skuPrefix(detail.getSkuPrefix())
                    .build();

            model.addAttribute("category", request);
            model.addAttribute("isEdit", true);
            model.addAttribute("categoryId", id);
            return "admin/category/form";
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/categories";
        }
    }

    @PostMapping("/edit/{id}")
    public String updateCategory(
            @PathVariable("id") Integer id,
            @Valid @ModelAttribute("category") ProductCategoryRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", true);
            model.addAttribute("categoryId", id);
            return "admin/category/form";
        }

        try {
            categoryService.update(id, request);
            redirectAttributes.addFlashAttribute("success", "Cập nhật danh mục ngành hàng thành công!");
            return "redirect:/admin/categories";
        } catch (ValidationException e) {
            if (e.getMessage().contains("SKU")) {
                bindingResult.rejectValue("skuPrefix", "duplicate", e.getMessage());
            } else if (e.getMessage().contains("danh mục") || e.getMessage().contains("Tên")) {
                bindingResult.rejectValue("categoryName", "duplicate", e.getMessage());
            } else {
                model.addAttribute("error", e.getMessage());
            }
            model.addAttribute("isEdit", true);
            model.addAttribute("categoryId", id);
            return "admin/category/form";
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteCategory(
            @PathVariable("id") Integer id,
            RedirectAttributes redirectAttributes) {

        try {
            categoryService.delete(id);
            redirectAttributes.addFlashAttribute("success", "Xóa danh mục ngành hàng thành công!");
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/categories";
    }
}

package com.retail.controller;

import com.retail.dto.SupplierRequest;
import com.retail.dto.SupplierResponse;
import com.retail.entity.SupplierStatus;
import com.retail.exception.ValidationException;
import com.retail.service.SupplierService;
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
@RequestMapping("/admin/suppliers")
public class SupplierController {

    @Autowired
    private SupplierService supplierService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    @GetMapping
    public String listSuppliers(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "status", required = false) SupplierStatus status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5") int size,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<SupplierResponse> supplierPage = supplierService.list(search, status, pageable);

        model.addAttribute("suppliers", supplierPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", supplierPage.getTotalPages());
        model.addAttribute("totalItems", supplierPage.getTotalElements());
        model.addAttribute("size", size);
        model.addAttribute("search", search);
        model.addAttribute("status", status);

        return "admin/supplier/list";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("supplier", new SupplierRequest());
        model.addAttribute("isEdit", false);
        return "admin/supplier/form";
    }

    @PostMapping("/create")
    public String createSupplier(
            @Valid @ModelAttribute("supplier") SupplierRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", false);
            return "admin/supplier/form";
        }

        try {
            supplierService.create(request);
            redirectAttributes.addFlashAttribute("success", "Thêm mới nhà cung cấp thành công!");
            return "redirect:/admin/suppliers";
        } catch (ValidationException e) {
            if (e.getMessage().contains("Email")) {
                bindingResult.rejectValue("contactEmail", "duplicate", e.getMessage());
            } else if (e.getMessage().contains("Số điện thoại") || e.getMessage().contains("Phone")) {
                bindingResult.rejectValue("contactPhone", "duplicate", e.getMessage());
            } else {
                model.addAttribute("error", e.getMessage());
            }
            model.addAttribute("isEdit", false);
            return "admin/supplier/form";
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(
            @PathVariable("id") Integer id,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            SupplierResponse detail = supplierService.getDetail(id);
            SupplierRequest request = SupplierRequest.builder()
                    .supplierName(detail.getSupplierName())
                    .contactPhone(detail.getContactPhone())
                    .contactEmail(detail.getContactEmail())
                    .address(detail.getAddress())
                    .build();

            model.addAttribute("supplier", request);
            model.addAttribute("isEdit", true);
            model.addAttribute("supplierId", id);
            return "admin/supplier/form";
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/suppliers";
        }
    }

    @PostMapping("/edit/{id}")
    public String updateSupplier(
            @PathVariable("id") Integer id,
            @Valid @ModelAttribute("supplier") SupplierRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", true);
            model.addAttribute("supplierId", id);
            return "admin/supplier/form";
        }

        try {
            supplierService.update(id, request);
            redirectAttributes.addFlashAttribute("success", "Cập nhật nhà cung cấp thành công!");
            return "redirect:/admin/suppliers";
        } catch (ValidationException e) {
            if (e.getMessage().contains("Email")) {
                bindingResult.rejectValue("contactEmail", "duplicate", e.getMessage());
            } else if (e.getMessage().contains("Số điện thoại") || e.getMessage().contains("Phone")) {
                bindingResult.rejectValue("contactPhone", "duplicate", e.getMessage());
            } else {
                model.addAttribute("error", e.getMessage());
            }
            model.addAttribute("isEdit", true);
            model.addAttribute("supplierId", id);
            return "admin/supplier/form";
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteSupplier(
            @PathVariable("id") Integer id,
            RedirectAttributes redirectAttributes) {

        try {
            supplierService.delete(id);
            redirectAttributes.addFlashAttribute("success", "Ngưng hoạt động (xóa logic) nhà cung cấp thành công!");
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/suppliers";
    }

    @PostMapping("/restore/{id}")
    public String restoreSupplier(
            @PathVariable("id") Integer id,
            RedirectAttributes redirectAttributes) {

        try {
            supplierService.restore(id);
            redirectAttributes.addFlashAttribute("success", "Khôi phục hoạt động nhà cung cấp thành công!");
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/suppliers";
    }
}

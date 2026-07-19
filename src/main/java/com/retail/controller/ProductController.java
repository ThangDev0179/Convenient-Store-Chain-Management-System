package com.retail.controller;

import com.retail.dto.ProductRequest;
import com.retail.dto.ProductResponse;
import com.retail.entity.ProductStatus;
import com.retail.exception.ValidationException;
import com.retail.repository.ProductCategoryRepository;
import com.retail.repository.SupplierRepository;
import com.retail.service.ProductService;
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
@RequestMapping("/admin/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductCategoryRepository categoryRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    @GetMapping
    public String listProducts(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "categoryId", required = false) Integer categoryId,
            @RequestParam(value = "supplierId", required = false) Integer supplierId,
            @RequestParam(value = "status", required = false) ProductStatus status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5") int size,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ProductResponse> productPage = productService.list(search, categoryId, supplierId, status, pageable);

        model.addAttribute("products", productPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("totalItems", productPage.getTotalElements());
        model.addAttribute("size", size);
        model.addAttribute("search", search);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("supplierId", supplierId);
        model.addAttribute("status", status);
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("suppliers", supplierRepository.findAll());

        return "admin/product/list";
    }

    @GetMapping("/detail/{id}")
    public String showDetail(
            @PathVariable("id") Long id,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            ProductResponse detail = productService.getDetail(id);
            model.addAttribute("product", detail);
            return "admin/product/detail";
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/products";
        }
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("product", new ProductRequest());
        model.addAttribute("isEdit", false);
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("suppliers", supplierRepository.findAll());
        return "admin/product/form";
    }

    @PostMapping("/create")
    public String createProduct(
            @Valid @ModelAttribute("product") ProductRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", false);
            model.addAttribute("categories", categoryRepository.findAll());
            model.addAttribute("suppliers", supplierRepository.findAll());
            return "admin/product/form";
        }

        try {
            productService.create(request);
            redirectAttributes.addFlashAttribute("success", "Thêm mới sản phẩm thành công!");
            return "redirect:/admin/products";
        } catch (ValidationException e) {
            if (e.getMessage().contains("Tên sản phẩm")) {
                bindingResult.rejectValue("productName", "duplicate", e.getMessage());
            } else if (e.getMessage().contains("Mã vạch") || e.getMessage().contains("barcode")) {
                bindingResult.rejectValue("barcode", "duplicate", e.getMessage());
            } else if (e.getMessage().contains("Danh mục")) {
                bindingResult.rejectValue("categoryId", "invalid", e.getMessage());
            } else {
                model.addAttribute("error", e.getMessage());
            }
            model.addAttribute("isEdit", false);
            model.addAttribute("categories", categoryRepository.findAll());
            model.addAttribute("suppliers", supplierRepository.findAll());
            return "admin/product/form";
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(
            @PathVariable("id") Long id,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            ProductResponse detail = productService.getDetail(id);
            ProductRequest request = ProductRequest.builder()
                    .productName(detail.getProductName())
                    .barcode(detail.getBarcode())
                    .description(detail.getDescription())
                    .categoryId(detail.getCategoryId())
                    .standardPrice(detail.getStandardPrice())
                    .defaultSupplierId(detail.getDefaultSupplierId())
                    .build();

            model.addAttribute("product", request);
            model.addAttribute("isEdit", true);
            model.addAttribute("productId", id);
            model.addAttribute("sku", detail.getSku());
            model.addAttribute("categories", categoryRepository.findAll());
            model.addAttribute("suppliers", supplierRepository.findAll());
            return "admin/product/form";
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/products";
        }
    }

    @PostMapping("/edit/{id}")
    public String updateProduct(
            @PathVariable("id") Long id,
            @Valid @ModelAttribute("product") ProductRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", true);
            model.addAttribute("productId", id);
            try {
                model.addAttribute("sku", productService.getDetail(id).getSku());
            } catch (Exception ignored) {}
            model.addAttribute("categories", categoryRepository.findAll());
            model.addAttribute("suppliers", supplierRepository.findAll());
            return "admin/product/form";
        }

        try {
            productService.update(id, request);
            redirectAttributes.addFlashAttribute("success", "Cập nhật sản phẩm thành công!");
            return "redirect:/admin/products";
        } catch (ValidationException e) {
            if (e.getMessage().contains("Tên sản phẩm")) {
                bindingResult.rejectValue("productName", "duplicate", e.getMessage());
            } else if (e.getMessage().contains("Mã vạch") || e.getMessage().contains("barcode")) {
                bindingResult.rejectValue("barcode", "duplicate", e.getMessage());
            } else {
                model.addAttribute("error", e.getMessage());
            }
            model.addAttribute("isEdit", true);
            model.addAttribute("productId", id);
            try {
                model.addAttribute("sku", productService.getDetail(id).getSku());
            } catch (Exception ignored) {}
            model.addAttribute("categories", categoryRepository.findAll());
            model.addAttribute("suppliers", supplierRepository.findAll());
            return "admin/product/form";
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteProduct(
            @PathVariable("id") Long id,
            RedirectAttributes redirectAttributes) {

        try {
            productService.delete(id);
            redirectAttributes.addFlashAttribute("success", "Ngưng hoạt động sản phẩm thành công!");
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/products";
    }

    @PostMapping("/restore/{id}")
    public String restoreProduct(
            @PathVariable("id") Long id,
            RedirectAttributes redirectAttributes) {

        try {
            productService.restore(id);
            redirectAttributes.addFlashAttribute("success", "Khôi phục hoạt động sản phẩm thành công!");
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/products";
    }
}

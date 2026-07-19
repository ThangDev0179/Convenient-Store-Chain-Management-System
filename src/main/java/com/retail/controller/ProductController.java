package com.retail.controller;

import com.retail.entity.Product;
import com.retail.entity.ProductStatus;
import com.retail.repository.ProductCategoryRepository;
import com.retail.repository.SupplierRepository;
import com.retail.service.ProductService;
import com.retail.exception.ValidationException;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/admin/products")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductCategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;

    @GetMapping
    public String listProducts(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "categoryId", required = false) Integer categoryId,
            @RequestParam(value = "supplierId", required = false) Integer supplierId,
            @RequestParam(value = "status", defaultValue = "Active") ProductStatus status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            Model model) {

        Page<Product> productPage = productService.list(search, categoryId, supplierId, status,
                PageRequest.of(page, size, Sort.by("productId").descending()));

        model.addAttribute("productPage", productPage);
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("suppliers", supplierRepository.findAll());
        model.addAttribute("search", search);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("supplierId", supplierId);
        model.addAttribute("status", status);
        model.addAttribute("currentPage", page);
        return "admin/products/product-list";
    }

    @PostMapping("/new")
    public String createProduct(
            @RequestParam("productName") String productName,
            @RequestParam("categoryId") Integer categoryId,
            @RequestParam("standardPrice") BigDecimal standardPrice,
            @RequestParam("supplierId") Integer supplierId,
            RedirectAttributes redirectAttributes) {
        try {
            Product product = productService.createProduct(productName, categoryId, standardPrice, supplierId);
            redirectAttributes.addFlashAttribute("success", "Thêm sản phẩm mới thành công! SKU: " + product.getSku());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/products";
    }
}

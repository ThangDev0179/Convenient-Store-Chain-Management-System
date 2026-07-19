package com.retail.controller;

import com.retail.entity.Supplier;
import com.retail.entity.SupplierStatus;
import com.retail.repository.SupplierRepository;
import com.retail.exception.ValidationException;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/suppliers")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierRepository supplierRepository;

    @GetMapping
    public String listSuppliers(Model model) {
        List<Supplier> suppliers = supplierRepository.findAll();
        model.addAttribute("suppliers", suppliers);
        return "admin/suppliers/supplier-list";
    }

    @PostMapping("/new")
    public String createSupplier(
            @RequestParam("supplierName") String name,
            @RequestParam("contactPhone") String phone,
            @RequestParam("contactEmail") String email,
            @RequestParam("address") String address,
            RedirectAttributes redirectAttributes) {
        try {
            if (name == null || name.trim().isEmpty()) {
                throw new ValidationException("Tên nhà cung cấp không được để trống");
            }
            Supplier supplier = Supplier.builder()
                    .supplierName(name.trim())
                    .contactPhone(phone != null ? phone.trim() : "")
                    .contactEmail(email != null ? email.trim() : "")
                    .address(address != null ? address.trim() : "")
                    .status(SupplierStatus.Active)
                    .build();
            supplierRepository.save(supplier);
            redirectAttributes.addFlashAttribute("success", "Thêm nhà cung cấp mới thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/suppliers";
    }
}

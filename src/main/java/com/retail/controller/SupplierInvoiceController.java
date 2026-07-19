package com.retail.controller;

import com.retail.dto.SupplierInvoiceResponse;
import com.retail.entity.Employee;
import com.retail.security.CustomUserDetails;
import com.retail.service.SupplierInvoiceService;
import com.retail.service.SupplierPaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/manager/invoices")
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public class SupplierInvoiceController {

    @Autowired
    private SupplierInvoiceService supplierInvoiceService;

    @Autowired
    private SupplierPaymentService supplierPaymentService;

    @GetMapping
    public String listInvoices(
            @RequestParam(value = "keyword", required = false) String keyword,
            @PageableDefault(size = 10) Pageable pageable,
            Model model) {
        Page<SupplierInvoiceResponse> invoicePage = supplierInvoiceService.searchInvoices(keyword, pageable);
        model.addAttribute("invoicePage", invoicePage);
        model.addAttribute("keyword", keyword);
        return "manager/purchase/invoice-list";
    }

    @GetMapping("/{id}")
    public String viewInvoiceDetail(@PathVariable("id") Long id, Model model) {
        SupplierInvoiceResponse invoice = supplierInvoiceService.getInvoiceById(id);
        model.addAttribute("invoice", invoice);
        model.addAttribute("payments", supplierPaymentService.getPaymentsByInvoiceId(id));
        return "manager/purchase/invoice-detail";
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @PostMapping("/{id}/approve")

    public String approveInvoice(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        try {
            Employee user = userDetails.getEmployee();
            supplierInvoiceService.approveInvoice(id, user.getEmployeeId());
            redirectAttributes.addFlashAttribute("success", "Duyệt hóa đơn mua hàng thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/manager/invoices/" + id;
    }
}

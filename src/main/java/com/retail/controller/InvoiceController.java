package com.retail.controller;

import com.retail.entity.Invoice;
import com.retail.entity.Product;
import com.retail.entity.Branch;
import com.retail.repository.ProductRepository;
import com.retail.repository.BranchRepository;
import com.retail.security.CustomUserDetails;
import com.retail.service.InvoiceService;
import com.retail.exception.ValidationException;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/staff/pos")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final ProductRepository productRepository;
    private final BranchRepository branchRepository;

    @GetMapping
    public String showPOSScreen(
            @RequestParam(value = "invoiceId", required = false) Long invoiceId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {

        Branch branch = userDetails.getEmployee().getBranch();
        if (branch == null) {
            model.addAttribute("error", "Nhân viên chưa được gán vào chi nhánh làm việc nào");
            return "error/403";
        }

        List<Product> products = productRepository.findAll();
        model.addAttribute("products", products);
        model.addAttribute("branchName", branch.getBranchName());

        if (invoiceId != null) {
            try {
                Invoice invoice = invoiceService.getDetail(invoiceId);
                model.addAttribute("invoice", invoice);
            } catch (Exception e) {
                model.addAttribute("error", e.getMessage());
            }
        }

        return "staff/pos";
    }

    @PostMapping("/new")
    public String createNewPOSSession(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        try {
            Branch branch = userDetails.getEmployee().getBranch();
            Invoice invoice = invoiceService.createDraftInvoice(branch.getBranchId(), userDetails.getEmployee().getEmployeeId());
            return "redirect:/staff/pos?invoiceId=" + invoice.getInvoiceId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/staff/pos";
        }
    }

    @PostMapping("/{id}/add-item")
    public String addItem(
            @PathVariable("id") Long invoiceId,
            @RequestParam("productId") Long productId,
            @RequestParam("quantity") BigDecimal quantity,
            RedirectAttributes redirectAttributes) {
        try {
            invoiceService.addDetail(invoiceId, productId, quantity);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/staff/pos?invoiceId=" + invoiceId;
    }

    @PostMapping("/{id}/checkout")
    public String checkout(
            @PathVariable("id") Long invoiceId,
            @RequestParam("paymentMethod") String paymentMethod,
            RedirectAttributes redirectAttributes) {
        try {
            Invoice invoice = invoiceService.checkout(invoiceId, paymentMethod);
            redirectAttributes.addFlashAttribute("success", "Thanh toán hóa đơn " + invoice.getInvoiceCode() + " thành công!");
            return "redirect:/staff/pos";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/staff/pos?invoiceId=" + invoiceId;
        }
    }

    @PostMapping("/{id}/cancel")
    public String cancel(
            @PathVariable("id") Long invoiceId,
            RedirectAttributes redirectAttributes) {
        try {
            invoiceService.cancel(invoiceId);
            redirectAttributes.addFlashAttribute("success", "Hủy hóa đơn thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/staff/pos";
    }
}

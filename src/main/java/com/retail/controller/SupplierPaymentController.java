package com.retail.controller;

import com.retail.dto.CreateSupplierPaymentRequest;
import com.retail.dto.SupplierInvoiceResponse;
import com.retail.dto.SupplierPaymentResponse;
import com.retail.entity.Employee;
import com.retail.security.CustomUserDetails;
import com.retail.service.SupplierInvoiceService;
import com.retail.service.SupplierPaymentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/manager/payments")
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public class SupplierPaymentController {

    @Autowired
    private SupplierPaymentService supplierPaymentService;

    @Autowired
    private SupplierInvoiceService supplierInvoiceService;

    @GetMapping
    public String listPayments(
            @RequestParam(value = "keyword", required = false) String keyword,
            @PageableDefault(size = 10) Pageable pageable,
            Model model) {
        Page<SupplierPaymentResponse> paymentPage = supplierPaymentService.searchPayments(keyword, pageable);
        model.addAttribute("paymentPage", paymentPage);
        model.addAttribute("keyword", keyword);
        return "manager/purchase/payment-list";
    }

    @GetMapping("/new")
    public String newPaymentForm(@RequestParam("invoiceId") Long invoiceId, Model model) {
        SupplierInvoiceResponse invoice = supplierInvoiceService.getInvoiceById(invoiceId);

        // Prepopulate request with max payable amount
        BigDecimal maxPayable = invoice.getAmount().subtract(invoice.getAmountPaid());
        CreateSupplierPaymentRequest paymentReq = CreateSupplierPaymentRequest.builder()
                .supplierInvoiceId(invoiceId)
                .amountPaid(maxPayable)
                .paymentMethod("Bank") // Default
                .build();

        model.addAttribute("paymentReq", paymentReq);
        model.addAttribute("invoice", invoice);
        return "manager/purchase/payment-form";
    }

    @PostMapping("/create")
    public String createPayment(
            @Valid @ModelAttribute("paymentReq") CreateSupplierPaymentRequest paymentReq,
            BindingResult result,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            SupplierInvoiceResponse invoice = supplierInvoiceService.getInvoiceById(paymentReq.getSupplierInvoiceId());
            model.addAttribute("invoice", invoice);
            return "manager/purchase/payment-form";
        }

        try {
            Employee user = userDetails.getEmployee();
            supplierPaymentService.createPayment(paymentReq, user.getEmployeeId());
            redirectAttributes.addFlashAttribute("success", "Lập phiếu chi thanh toán thành công.");
            return "redirect:/manager/invoices/" + paymentReq.getSupplierInvoiceId();
        } catch (Exception e) {
            SupplierInvoiceResponse invoice = supplierInvoiceService.getInvoiceById(paymentReq.getSupplierInvoiceId());
            model.addAttribute("invoice", invoice);
            model.addAttribute("error", e.getMessage());
            return "manager/purchase/payment-form";
        }
    }
}

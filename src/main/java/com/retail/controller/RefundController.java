package com.retail.controller;

import com.retail.entity.Refund;
import com.retail.entity.Invoice;
import com.retail.entity.Product;
import com.retail.repository.InvoiceRepository;
import com.retail.repository.EmployeeRepository;
import com.retail.security.CustomUserDetails;
import com.retail.service.RefundService;
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
@RequestMapping("/staff/refund")
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;
    private final InvoiceRepository invoiceRepository;
    private final EmployeeRepository employeeRepository;

    @GetMapping
    public String showRefundScreen(
            @RequestParam(value = "refundId", required = false) Long refundId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {

        Integer branchId = userDetails.getEmployee().getBranch() != null ?
                userDetails.getEmployee().getBranch().getBranchId() : null;

        if (branchId == null) {
            model.addAttribute("error", "Nhân viên chưa được gán vào chi nhánh làm việc nào");
            return "error/403";
        }

        // Chỉ lấy các hóa đơn Paid của chi nhánh
        List<Invoice> invoices = invoiceRepository.findAll().stream()
                .filter(i -> i.getBranch().getBranchId().equals(branchId) && "Paid".equals(i.getStatus()))
                .toList();
        model.addAttribute("invoices", invoices);

        // Lấy tất cả quản lý cùng chi nhánh để phê duyệt khi cần
        var managers = employeeRepository.findByBranchBranchIdAndStatus(branchId, com.retail.entity.EmployeeStatus.Active).stream()
                .filter(e -> "MANAGER".equals(e.getRole().getRoleCode().name()))
                .toList();
        model.addAttribute("managers", managers);

        if (refundId != null) {
            try {
                Refund refund = refundService.getDetail(refundId);
                model.addAttribute("refund", refund);
                model.addAttribute("originalInvoice", refund.getOriginalInvoice());
            } catch (Exception e) {
                model.addAttribute("error", e.getMessage());
            }
        }

        return "staff/refund";
    }

    @PostMapping("/new")
    public String createNewRefund(
            @RequestParam("originalInvoiceId") Long originalInvoiceId,
            @RequestParam("customerName") String customerName,
            @RequestParam("customerPhone") String customerPhone,
            @RequestParam("reason") String reason,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        try {
            Refund refund = refundService.createRefund(originalInvoiceId, customerName, customerPhone, reason, userDetails.getEmployee().getEmployeeId());
            return "redirect:/staff/refund?refundId=" + refund.getRefundId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/staff/refund";
        }
    }

    @PostMapping("/{id}/add-item")
    public String addItem(
            @PathVariable("id") Long refundId,
            @RequestParam("productId") Long productId,
            @RequestParam("quantity") BigDecimal quantity,
            @RequestParam("conditionType") String conditionType,
            RedirectAttributes redirectAttributes) {
        try {
            refundService.addDetail(refundId, productId, quantity, conditionType);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/staff/refund?refundId=" + refundId;
    }

    @PostMapping("/{id}/approve")
    public String approve(
            @PathVariable("id") Long refundId,
            @RequestParam(value = "managerEmployeeId", required = false) Long managerEmployeeId,
            @RequestParam(value = "managerPinOverride", required = false) String managerPinOverride,
            RedirectAttributes redirectAttributes) {
        try {
            Refund refund = refundService.approveRefund(refundId, managerEmployeeId, managerPinOverride);
            redirectAttributes.addFlashAttribute("success", "Phê duyệt phiếu đổi trả hoàn tiền " + refund.getRefundCode() + " thành công!");
            return "redirect:/staff/refund";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/staff/refund?refundId=" + refundId;
        }
    }
}

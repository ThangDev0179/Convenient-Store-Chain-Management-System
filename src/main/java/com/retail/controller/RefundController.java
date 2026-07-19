package com.retail.controller;

import com.retail.dto.*;
import com.retail.dto.RefundResponse;
import com.retail.entity.RefundStatus;
import com.retail.service.RefundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for Refund (Ä‘á»•i/tráº£ hÃ ng) module.
 *
 * Security (Section 8):
 *   - Táº¡o refund:      STAFF, MANAGER, ADMIN
 *   - Approve/Reject:  MANAGER, ADMIN only
 *   - Override PIN:    MANAGER, ADMIN (re-authenticated via PIN in service)
 *   - STAFF view:      only own branch (enforced in Service)
 *
 * URL structure:
 *   GET  /refunds              â†’ list (Thymeleaf)
 *   GET  /refunds/create       â†’ create form (Thymeleaf)
 *   GET  /refunds/{id}         â†’ detail (Thymeleaf)
 *   POST /refunds              â†’ create refund (form submit)
 *   PUT  /refunds/{id}/approve â†’ approve (AJAX JSON)
 *   PUT  /refunds/{id}/reject  â†’ reject (AJAX JSON)
 *   PUT  /refunds/{id}/override-approve â†’ PIN override (AJAX JSON)
 */
@Controller
@RequestMapping("/refunds")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class RefundController {

    private final RefundService refundService;

    // â”€â”€â”€ List page â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @GetMapping
    @PreAuthorize("hasAnyRole('STAFF','MANAGER','ADMIN')")
    public String listRefunds(@ModelAttribute RefundSearchRequest request, Model model) {
        Page<RefundResponse> page = refundService.listRefunds(request);
        model.addAttribute("refundPage", page);
        model.addAttribute("searchRequest", request);
        model.addAttribute("statuses", RefundStatus.values());
        return "refund/list";
    }

    // â”€â”€â”€ Create form â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @GetMapping("/create")
    @PreAuthorize("hasAnyRole('STAFF','MANAGER','ADMIN')")
    public String createForm(@RequestParam(required = false) String invoiceCode, Model model) {
        model.addAttribute("invoiceCode", invoiceCode);
        model.addAttribute("conditionTypes",
                com.retail.entity.ConditionType.values());
        return "refund/create";
    }

    // â”€â”€â”€ Detail page â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STAFF','MANAGER','ADMIN')")
    public String refundDetail(@PathVariable Long id, Model model) {
        RefundResponse refund = refundService.getRefundDetail(id);
        model.addAttribute("refund", refund);
        return "refund/detail";
    }

    // â”€â”€â”€ Create (form POST â†’ Thymeleaf redirect) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @PostMapping
    @PreAuthorize("hasAnyRole('STAFF','MANAGER','ADMIN')")
    public String createRefund(@Valid @ModelAttribute CreateRefundRequest request,
                                org.springframework.validation.BindingResult bindingResult,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("conditionTypes", com.retail.entity.ConditionType.values());
            return "refund/create";
        }
        try {
            RefundResponse created = refundService.createRefund(request);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Refund " + created.refundCode() + " created successfully with status: " + created.status());
            return "redirect:/refunds/" + created.refundId();
        } catch (Exception ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("conditionTypes", com.retail.entity.ConditionType.values());
            return "refund/create";
        }
    }

    // â”€â”€â”€ Approve (AJAX JSON) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @ResponseBody
    public ResponseEntity<RefundResponse> approveRefund(@PathVariable Long id) {
        return ResponseEntity.ok(refundService.approveRefund(id));
    }

    // â”€â”€â”€ Reject (AJAX JSON) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @ResponseBody
    public ResponseEntity<RefundResponse> rejectRefund(@PathVariable Long id,
                                                        @Valid @RequestBody RejectRefundRequest request) {
        return ResponseEntity.ok(refundService.rejectRefund(id, request.reason()));
    }

    // â”€â”€â”€ Manager PIN Override at POS (AJAX JSON) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @PutMapping("/{id}/override-approve")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @ResponseBody
    public ResponseEntity<RefundResponse> overrideApprove(@PathVariable Long id,
                                                           @Valid @RequestBody RefundOverrideApproveRequest request) {
        return ResponseEntity.ok(refundService.overrideApprove(id, request));
    }

}

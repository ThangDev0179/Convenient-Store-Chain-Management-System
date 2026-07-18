package com.retail.disposal.controller;

import com.retail.disposal.dto.StockDisposalRequest;
import com.retail.disposal.entity.StockDisposal;
import com.retail.disposal.service.StockDisposalService;
import com.retail.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/disposal")
@RequiredArgsConstructor
public class StockDisposalController {

    private final StockDisposalService disposalService;

    private Long getEmployeeId(Authentication auth) {
        return ((CustomUserDetails) auth.getPrincipal()).getEmployee().getEmployeeId();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    public String listDisposals(Model model) {
        List<StockDisposal> disposals = disposalService.getAllDisposals();
        model.addAttribute("disposals", disposals);
        return "disposal/list";
    }

    @GetMapping("/create")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public String showCreateForm(Model model) {
        model.addAttribute("request", new StockDisposalRequest());
        return "disposal/create";
    }

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public String createDisposal(@Valid @ModelAttribute("request") StockDisposalRequest request,
                                 BindingResult result,
                                 Authentication auth,
                                 RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "disposal/create";
        }
        try {
            disposalService.createManualDisposal(request, getEmployeeId(auth));
            redirectAttributes.addFlashAttribute("successMessage", "Tạo phiếu xuất hủy thủ công thành công.");
            return "redirect:/disposal";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/disposal/create";
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    public String viewDisposal(@PathVariable Long id, Model model) {
        StockDisposal disposal = disposalService.getDisposalById(id);
        model.addAttribute("disposal", disposal);
        return "disposal/detail";
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public String approveDisposal(@PathVariable Long id, Authentication auth, RedirectAttributes redirectAttributes) {
        try {
            disposalService.approveDisposal(id, getEmployeeId(auth));
            redirectAttributes.addFlashAttribute("successMessage", "Duyệt phiếu xuất hủy thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/disposal/" + id;
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public String rejectDisposal(@PathVariable Long id, Authentication auth, RedirectAttributes redirectAttributes) {
        try {
            disposalService.rejectDisposal(id, getEmployeeId(auth));
            redirectAttributes.addFlashAttribute("successMessage", "Đã từ chối phiếu xuất hủy.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/disposal/" + id;
    }
}

package com.retail.controller;

import com.retail.dto.ReceiveTransferRequest;
import com.retail.dto.StockTransferRequest;
import com.retail.entity.StockTransfer;
import com.retail.repository.BranchRepository;
import com.retail.repository.ProductRepository;
import com.retail.security.CustomUserDetails;
import com.retail.service.StockTransferService;
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
@RequestMapping("/transfer")
@RequiredArgsConstructor
public class StockTransferController {

    private final StockTransferService transferService;
    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;

    private Long getEmployeeId(Authentication auth) {
        return ((CustomUserDetails) auth.getPrincipal()).getEmployee().getEmployeeId();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    public String listTransfers(Model model) {
        List<StockTransfer> transfers = transferService.getAllTransfers();
        model.addAttribute("transfers", transfers);
        return "transfer/list";
    }

    @GetMapping("/create")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    public String showCreateForm(Model model, Authentication auth) {
        StockTransferRequest request = new StockTransferRequest();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails userDetails) {
            if (userDetails.getEmployee() != null && userDetails.getEmployee().getBranch() != null) {
                request.setFromBranchId(userDetails.getEmployee().getBranch().getBranchId());
            }
        }
        model.addAttribute("request", request);
        model.addAttribute("branches", branchRepository.findAll());
        model.addAttribute("products", productRepository.findAll());
        return "transfer/create";
    }

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    public String createTransfer(@Valid @ModelAttribute("request") StockTransferRequest request,
                                 BindingResult result,
                                 Authentication auth,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("branches", branchRepository.findAll());
            model.addAttribute("products", productRepository.findAll());
            return "transfer/create";
        }
        try {
            transferService.createTransfer(request, getEmployeeId(auth));
            redirectAttributes.addFlashAttribute("successMessage", "Tạo phiếu điều chuyển thành công.");
            return "redirect:/transfer";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/transfer/create";
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    public String viewTransfer(@PathVariable Long id, Model model) {
        StockTransfer transfer = transferService.getTransferById(id);
        model.addAttribute("transfer", transfer);
        model.addAttribute("receiveRequest", new ReceiveTransferRequest());
        return "transfer/detail";
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public String approveTransfer(@PathVariable Long id, Authentication auth,
                                  RedirectAttributes redirectAttributes) {
        try {
            transferService.approveTransfer(id, getEmployeeId(auth));
            redirectAttributes.addFlashAttribute("successMessage", "Phê duyệt phiếu điều chuyển thành công. Hàng đang được vận chuyển.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/transfer/" + id;
    }

    @PostMapping("/{id}/receive")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    public String receiveTransfer(@PathVariable Long id,
                                  @Valid @ModelAttribute("receiveRequest") ReceiveTransferRequest request,
                                  BindingResult result,
                                  Authentication auth,
                                  RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Dữ liệu nhận hàng không hợp lệ.");
            return "redirect:/transfer/" + id;
        }
        try {
            transferService.receiveTransfer(id, request, getEmployeeId(auth));
            redirectAttributes.addFlashAttribute("successMessage", "Xác nhận nhận hàng thành công. Tồn kho đã được cập nhật.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/transfer/" + id;
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public String rejectTransfer(@PathVariable Long id, Authentication auth,
                                 RedirectAttributes redirectAttributes) {
        try {
            transferService.rejectTransfer(id, getEmployeeId(auth));
            redirectAttributes.addFlashAttribute("successMessage", "Đã từ chối phiếu điều chuyển.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/transfer/" + id;
    }
}
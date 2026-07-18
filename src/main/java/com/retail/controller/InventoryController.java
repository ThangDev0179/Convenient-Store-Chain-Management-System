package com.retail.controller;

import com.retail.dto.BranchInventoryResponse;
import com.retail.dto.InventoryTransactionResponse;
import com.retail.entity.Branch;
import com.retail.entity.Employee;
import com.retail.security.CustomUserDetails;
import com.retail.repository.BranchRepository;
import com.retail.repository.ProductRepository;
import com.retail.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Controller
@RequestMapping("/manager/inventory")
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private ProductRepository productRepository;

    @GetMapping
    public String viewInventory(
            @RequestParam(value = "branchId", required = false) Integer branchId,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "categoryId", required = false) Integer categoryId,
            @PageableDefault(size = 15) Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {
        
        Employee employee = userDetails.getEmployee();
        boolean isAdmin = employee.getRole().getRoleCode().name().equals("ADMIN");
        
        // If not Admin, force to their own branch
        if (!isAdmin) {
            branchId = employee.getBranch().getBranchId();
        }

        Page<BranchInventoryResponse> inventoryPage = inventoryService.searchInventory(branchId, keyword, categoryId, pageable);
        
        model.addAttribute("inventoryPage", inventoryPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("branchId", branchId);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("isAdmin", isAdmin);
        
        if (isAdmin) {
            model.addAttribute("branches", branchRepository.findAll());
        }

        return "manager/inventory/inventory-list";
    }

    @GetMapping("/ledger")
    public String viewLedger(
            @RequestParam(value = "branchId", required = false) Integer branchId,
            @RequestParam(value = "productId", required = false) Long productId,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "transactionType", required = false) String transactionType,
            @PageableDefault(size = 15) Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {

        Employee employee = userDetails.getEmployee();
        boolean isAdmin = employee.getRole().getRoleCode().name().equals("ADMIN");

        // Force branch filter if not Admin
        if (!isAdmin) {
            branchId = employee.getBranch().getBranchId();
        }

        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = endDate != null ? endDate.atTime(LocalTime.MAX) : null;

        Page<InventoryTransactionResponse> ledgerPage = inventoryService.filterHistory(
                branchId, productId, startDateTime, endDateTime, transactionType, pageable);

        model.addAttribute("ledgerPage", ledgerPage);
        model.addAttribute("branchId", branchId);
        model.addAttribute("productId", productId);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("transactionType", transactionType);
        model.addAttribute("isAdmin", isAdmin);

        if (isAdmin) {
            model.addAttribute("branches", branchRepository.findAll());
        }
        
        // Add list of products for drop-down filter
        model.addAttribute("products", productRepository.findAll());

        return "manager/inventory/ledger-list";
    }
}

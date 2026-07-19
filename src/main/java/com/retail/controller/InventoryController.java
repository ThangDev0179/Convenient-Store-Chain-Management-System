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

    @GetMapping("/ledger/export")
    public void exportLedgerCsv(
            @RequestParam(value = "branchId", required = false) Integer branchId,
            @RequestParam(value = "productId", required = false) Long productId,
            @RequestParam(value = "startDate", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "transactionType", required = false) String transactionType,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {

        Employee employee = userDetails.getEmployee();
        boolean isAdmin = employee.getRole().getRoleCode().name().equals("ADMIN");

        if (!isAdmin) {
            branchId = employee.getBranch().getBranchId();
        }

        java.time.LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        java.time.LocalDateTime endDateTime = endDate != null ? endDate.atTime(java.time.LocalTime.MAX) : null;

        Page<InventoryTransactionResponse> ledgerPage = inventoryService.filterHistory(
                branchId, productId, startDateTime, endDateTime, transactionType, org.springframework.data.domain.PageRequest.of(0, 10000));

        response.setContentType("text/csv; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=inventory_ledger.csv");

        // Write UTF-8 BOM so Excel displays Vietnamese correctly
        response.getWriter().write('\ufeff');

        java.io.PrintWriter writer = response.getWriter();
        writer.println("Mã Giao Dịch,Thời Gian,Chi Nhánh,Sản Phẩm,Mã SKU,Loại Giao Dịch,Chứng Từ Nguồn,Biến Động (Base Unit),Lý Do / Ghi Chú,Người Thực Hiện");

        for (InventoryTransactionResponse tx : ledgerPage.getContent()) {
            writer.printf("TX-%d,%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
                    tx.getTransactionId(),
                    tx.getCreatedAt() != null ? tx.getCreatedAt().toString() : "",
                    escapeCsv(tx.getBranchName()),
                    escapeCsv(tx.getProductName()),
                    escapeCsv(tx.getProductSku()),
                    tx.getTransactionType(),
                    tx.getReferenceTable() + " #" + tx.getReferenceId(),
                    tx.getQuantityChange().toString(),
                    escapeCsv(tx.getReason()),
                    escapeCsv(tx.getCreatedByName())
            );
        }
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }
}

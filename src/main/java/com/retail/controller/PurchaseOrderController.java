package com.retail.controller;

import com.retail.dto.CreatePurchaseOrderRequest;
import com.retail.dto.PurchaseOrderResponse;
import com.retail.entity.Branch;
import com.retail.entity.Employee;
import com.retail.entity.Product;
import com.retail.entity.Supplier;
import com.retail.entity.SupplierStatus;
import com.retail.exception.ValidationException;
import com.retail.repository.BranchRepository;
import com.retail.repository.ProductRepository;
import com.retail.repository.SupplierRepository;
import com.retail.security.CustomUserDetails;
import com.retail.service.PurchaseOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/manager/purchase-orders")
public class PurchaseOrderController {

    @Autowired
    private PurchaseOrderService poService;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private com.retail.repository.ProductUOMRepository productUOMRepository;

    @GetMapping
    public String listPurchaseOrders(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "branchId", required = false) Integer filterBranchId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            Model model) {

        Employee employee = userDetails.getEmployee();
        Integer branchId = null;

        // If employee is MANAGER, restrict to their branch
        if (!employee.getRole().getRoleCode().equals("ADMIN")) {
            branchId = employee.getBranch().getBranchId();
            model.addAttribute("isManager", true);
        } else {
            // ADMIN can filter by branch
            branchId = filterBranchId;
            model.addAttribute("isManager", false);
            model.addAttribute("branches", branchRepository.findAll());
        }

        Page<PurchaseOrderResponse> poPage = poService.searchPurchaseOrders(branchId, status, keyword, page, size);

        model.addAttribute("poPage", poPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("selectedBranchId", filterBranchId);
        
        return "manager/purchase/po-list";
    }

    @GetMapping("/{id}")
    public String viewPurchaseOrder(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            PurchaseOrderResponse po = poService.getPurchaseOrderById(id);
            Employee employee = userDetails.getEmployee();

            // Verify branch access
            if (!employee.getRole().getRoleCode().equals("ADMIN") 
                && !po.getBranchId().equals(employee.getBranch().getBranchId())) {
                throw new ValidationException("Bạn không có quyền truy cập đơn đặt hàng của chi nhánh khác");
            }

            model.addAttribute("po", po);
            return "manager/purchase/po-detail";
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/manager/purchase-orders";
        }
    }

    @GetMapping("/new")
    public String newPurchaseOrderForm(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {
        
        Employee employee = userDetails.getEmployee();
        
        List<Supplier> suppliers = supplierRepository.findByStatus(SupplierStatus.Active);
        List<Product> products = productRepository.findAll(); // simplified for selecting products
        
        CreatePurchaseOrderRequest poRequest = new CreatePurchaseOrderRequest();
        poRequest.setBranchId(employee.getBranch().getBranchId());

        model.addAttribute("poRequest", poRequest);
        model.addAttribute("suppliers", suppliers);
        model.addAttribute("products", products);
        model.addAttribute("uoms", productUOMRepository.findAll());
        model.addAttribute("branch", employee.getBranch());
        
        return "manager/purchase/po-form";
    }

    @PostMapping("/create")
    public String createPurchaseOrder(
            @Valid @ModelAttribute("poRequest") CreatePurchaseOrderRequest poRequest,
            BindingResult result,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model,
            RedirectAttributes redirectAttributes) {

        Employee employee = userDetails.getEmployee();

        if (result.hasErrors()) {
            model.addAttribute("suppliers", supplierRepository.findByStatus(SupplierStatus.Active));
            model.addAttribute("products", productRepository.findAll());
            model.addAttribute("uoms", productUOMRepository.findAll());
            model.addAttribute("branch", employee.getBranch());
            model.addAttribute("error", "Dữ liệu nhập vào không hợp lệ. Vui lòng kiểm tra lại.");
            return "manager/purchase/po-form";
        }

        try {
            // Force manager's branch
            if (!employee.getRole().getRoleCode().equals("ADMIN")) {
                poRequest.setBranchId(employee.getBranch().getBranchId());
            }

            PurchaseOrderResponse response = poService.createPurchaseOrder(poRequest, employee);
            redirectAttributes.addFlashAttribute("success", "Tạo đơn đặt hàng nháp thành công: " + response.getPoCode());
            return "redirect:/manager/purchase-orders/" + response.getPurchaseOrderId();
        } catch (ValidationException e) {
            model.addAttribute("suppliers", supplierRepository.findByStatus(SupplierStatus.Active));
            model.addAttribute("products", productRepository.findAll());
            model.addAttribute("uoms", productUOMRepository.findAll());
            model.addAttribute("branch", employee.getBranch());
            model.addAttribute("error", e.getMessage());
            return "manager/purchase/po-form";
        }
    }

    @PostMapping("/{id}/submit")
    public String submitPurchaseOrder(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        try {
            poService.submitPurchaseOrder(id, userDetails.getEmployee());
            redirectAttributes.addFlashAttribute("success", "Đã gửi phê duyệt đơn đặt hàng.");
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/manager/purchase-orders/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancelPurchaseOrder(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        try {
            poService.cancelPurchaseOrder(id, userDetails.getEmployee());
            redirectAttributes.addFlashAttribute("success", "Đã hủy đơn đặt hàng.");
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/manager/purchase-orders/" + id;
    }
}

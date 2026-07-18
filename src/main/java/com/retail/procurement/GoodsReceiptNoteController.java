package com.retail.procurement;

import com.retail.employee.Employee;
import com.retail.exception.ValidationException;
import com.retail.procurement.dto.CreateGoodsReceiptNoteRequest;
import com.retail.procurement.dto.GoodsReceiptNoteDetailRequest;
import com.retail.procurement.dto.GoodsReceiptNoteResponse;
import com.retail.procurement.dto.PurchaseOrderResponse;
import com.retail.security.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/manager/grn")
public class GoodsReceiptNoteController {

    @Autowired
    private GoodsReceiptNoteService grnService;

    @Autowired
    private PurchaseOrderService poService;

    @GetMapping
    public String listGoodsReceiptNotes(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {

        Employee employee = userDetails.getEmployee();
        Integer branchId = null;

        // Managers only see their own branch's GRNs, Admins see all
        if (!employee.getRole().getRoleCode().equals("ADMIN")) {
            branchId = employee.getBranch().getBranchId();
        }

        Page<GoodsReceiptNoteResponse> grnPage = grnService.searchGoodsReceiptNotes(branchId, keyword, page, size);

        model.addAttribute("grnPage", grnPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentPage", page);
        return "manager/purchase/grn-list";
    }

    @GetMapping("/{id}")
    public String viewGoodsReceiptNote(
            @PathVariable("id") Long id,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            GoodsReceiptNoteResponse grn = grnService.getGoodsReceiptNoteById(id);
            model.addAttribute("grn", grn);
            return "manager/purchase/grn-detail";
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/manager/grn";
        }
    }

    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping("/new")
    public String newGoodsReceiptNoteForm(
            @RequestParam("poId") Long poId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            Employee employee = userDetails.getEmployee();
            PurchaseOrderResponse po = poService.getPurchaseOrderById(poId);

            // Verify PO branch matches manager's branch
            if (!po.getBranchId().equals(employee.getBranch().getBranchId())) {
                throw new ValidationException("Bạn chỉ có thể lập phiếu nhập kho cho đơn hàng thuộc chi nhánh của mình.");
            }

            if (!po.getStatus().equals("Submitted") && !po.getStatus().equals("Partially_Received")) {
                throw new ValidationException("Chỉ đơn hàng ở trạng thái Đã gửi duyệt hoặc Đang nhập kho dở dang mới được tiếp nhận.");
            }

            CreateGoodsReceiptNoteRequest grnRequest = new CreateGoodsReceiptNoteRequest();
            grnRequest.setPurchaseOrderId(po.getPurchaseOrderId());
            grnRequest.setBranchId(employee.getBranch().getBranchId());

            // Pre-populate details from PO
            List<GoodsReceiptNoteDetailRequest> details = po.getDetails().stream().map(d -> 
                GoodsReceiptNoteDetailRequest.builder()
                        .productId(d.getProductId())
                        .uomId(d.getUomId())
                        .quantityReceived(d.getQuantityOrdered()) // Default to ordered quantity
                        .build()
            ).collect(Collectors.toList());

            grnRequest.setDetails(details);

            model.addAttribute("grnRequest", grnRequest);
            model.addAttribute("po", po);
            model.addAttribute("branch", employee.getBranch());
            return "manager/purchase/grn-form";
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/manager/purchase-orders/" + poId;
        }
    }

    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping("/create")
    public String createGoodsReceiptNote(
            @Valid @ModelAttribute("grnRequest") CreateGoodsReceiptNoteRequest grnRequest,
            BindingResult result,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model,
            RedirectAttributes redirectAttributes) {

        Employee employee = userDetails.getEmployee();

        if (result.hasErrors()) {
            PurchaseOrderResponse po = poService.getPurchaseOrderById(grnRequest.getPurchaseOrderId());
            model.addAttribute("po", po);
            model.addAttribute("branch", employee.getBranch());
            model.addAttribute("error", "Dữ liệu nhập vào không hợp lệ. Vui lòng kiểm tra.");
            return "manager/purchase/grn-form";
        }

        try {
            grnRequest.setBranchId(employee.getBranch().getBranchId());
            GoodsReceiptNoteResponse response = grnService.createGoodsReceiptNote(grnRequest, employee);
            redirectAttributes.addFlashAttribute("success", "Tạo phiếu nhập kho thành công: " + response.getGrnCode());
            return "redirect:/manager/grn/" + response.getGrnId();
        } catch (ValidationException e) {
            PurchaseOrderResponse po = poService.getPurchaseOrderById(grnRequest.getPurchaseOrderId());
            model.addAttribute("po", po);
            model.addAttribute("branch", employee.getBranch());
            model.addAttribute("error", e.getMessage());
            return "manager/purchase/grn-form";
        }
    }
}

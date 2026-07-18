package com.retail.shift;

import com.retail.shift.dto.CashClosingRequest;
import com.retail.shift.dto.CheckInRequest;
import com.retail.shift.dto.CheckOutRequest;
import com.retail.exception.AlreadyCheckedInException;
import com.retail.exception.ValidationException;
import com.retail.security.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;
import java.util.Optional;

@Controller
public class WorkShiftController {

    @Autowired
    private WorkShiftService workShiftService;

    @GetMapping("/staff/shift")
    public String shiftMain(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        Long employeeId = userDetails.getEmployee().getEmployeeId();
        Optional<WorkShift> activeShift = workShiftService.findActiveShiftForEmployee(employeeId);

        if (activeShift.isPresent()) {
            model.addAttribute("activeShift", activeShift.get());
            model.addAttribute("isOpen", true);
        } else {
            model.addAttribute("isOpen", false);
        }
        return "staff/shift/shift-checkin";
    }

    @PostMapping("/staff/shift/check-in")
    public String checkIn(@AuthenticationPrincipal CustomUserDetails userDetails, RedirectAttributes redirectAttributes) {
        try {
            CheckInRequest request = CheckInRequest.builder()
                    .employeeId(userDetails.getEmployee().getEmployeeId())
                    .branchId(userDetails.getEmployee().getBranch().getBranchId())
                    .build();
            workShiftService.checkIn(request);
            redirectAttributes.addFlashAttribute("success", "Check-in thành công!");
        } catch (AlreadyCheckedInException | ValidationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/staff/shift";
    }

    @PostMapping("/staff/shift/check-out")
    public String checkOut(@AuthenticationPrincipal CustomUserDetails userDetails, RedirectAttributes redirectAttributes) {
        try {
            Optional<WorkShift> activeShift = workShiftService.findActiveShiftForEmployee(userDetails.getEmployee().getEmployeeId());
            if (activeShift.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy ca làm việc đang mở để check-out.");
                return "redirect:/staff/shift";
            }
            CheckOutRequest request = new CheckOutRequest(activeShift.get().getWorkShiftId());
            WorkShift checkedOut = workShiftService.checkOut(request);
            return "redirect:/staff/shift/cash-closing?workShiftId=" + checkedOut.getWorkShiftId();
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/staff/shift";
        }
    }

    @GetMapping("/staff/shift/cash-closing")
    public String showCashClosing(@RequestParam("workShiftId") Long workShiftId, Model model, RedirectAttributes redirectAttributes) {
        try {
            WorkShift ws = workShiftService.getDetail(workShiftId);
            model.addAttribute("workShiftId", workShiftId);
            model.addAttribute("workShift", ws);
            return "staff/shift/cash-closing";
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/staff/shift";
        }
    }

    @PostMapping("/staff/shift/cash-closing")
    public String submitCashClosing(
            @ModelAttribute("cashRequest") CashClosingRequest request,
            RedirectAttributes redirectAttributes) {
        try {
            workShiftService.cashClosing(request);
            redirectAttributes.addFlashAttribute("success", "Khai báo kết ca bàn giao tiền thành công!");
            return "redirect:/staff/shift";
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/staff/shift/cash-closing?workShiftId=" + request.getWorkShiftId();
        }
    }

    @GetMapping("/staff/shift/history")
    public String viewHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5") int size,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("checkInTime").descending());
        Page<WorkShift> shiftPage = workShiftService.list(
                null, null, userDetails.getEmployee().getEmployeeId(), startDate, endDate, pageable);

        model.addAttribute("shifts", shiftPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", shiftPage.getTotalPages());
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

        return "staff/shift/shift-history";
    }

    @GetMapping("/manager/shift-history")
    public String viewBranchHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            Model model) {

        Integer branchId = userDetails.getEmployee().getBranch() != null ?
                userDetails.getEmployee().getBranch().getBranchId() : null;

        Pageable pageable = PageRequest.of(page, size, Sort.by("checkInTime").descending());
        Page<WorkShift> shiftPage = workShiftService.list(search, branchId, null, startDate, endDate, pageable);

        model.addAttribute("shifts", shiftPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", shiftPage.getTotalPages());
        model.addAttribute("search", search);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

        return "manager/schedule/branch-shift-history";
    }
}

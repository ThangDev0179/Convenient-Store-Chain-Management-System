package com.retail.shift;

import com.retail.branch.Branch;
import com.retail.branch.BranchRepository;
import com.retail.employee.Employee;
import com.retail.employee.EmployeeRepository;
import com.retail.shift.dto.ScheduleRequest;
import com.retail.exception.ValidationException;
import com.retail.security.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/manager/schedule")
public class ScheduleController {

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private ShiftTypeRepository shiftTypeRepository;

    @GetMapping
    public String listSchedules(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "employeeId", required = false) Long employeeId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {

        String userRole = userDetails.getEmployee().getRole().getRoleCode().name();
        Integer branchId = userDetails.getEmployee().getBranch() != null ?
                userDetails.getEmployee().getBranch().getBranchId() : null;

        if ("MANAGER".equals(userRole)) {
            model.addAttribute("isManagerUser", true);
        } else {
            branchId = null;
            model.addAttribute("isManagerUser", false);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<EmployeeShiftSchedule> schedulePage = scheduleService.list(search, branchId, employeeId, pageable);

        model.addAttribute("schedules", schedulePage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", schedulePage.getTotalPages());
        model.addAttribute("search", search);
        model.addAttribute("employeeId", employeeId);

        return "manager/schedule/schedule-list";
    }

    @GetMapping("/new")
    public String showCreateForm(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        String userRole = userDetails.getEmployee().getRole().getRoleCode().name();
        Integer managerBranchId = userDetails.getEmployee().getBranch() != null ?
                userDetails.getEmployee().getBranch().getBranchId() : null;

        ScheduleRequest request = new ScheduleRequest();
        if ("MANAGER".equals(userRole)) {
            request.setBranchId(managerBranchId);
            model.addAttribute("branches", branchRepository.findById(managerBranchId).stream().collect(Collectors.toList()));
            model.addAttribute("employees", employeeRepository.findAll().stream()
                    .filter(e -> e.getBranch() != null && e.getBranch().getBranchId().equals(managerBranchId))
                    .collect(Collectors.toList()));
        } else {
            model.addAttribute("branches", branchRepository.findAll());
            model.addAttribute("employees", employeeRepository.findAll());
        }

        model.addAttribute("shiftTypes", shiftTypeRepository.findAll());
        model.addAttribute("scheduleRequest", request);
        model.addAttribute("isEdit", false);
        return "manager/schedule/schedule-form";
    }

    @PostMapping("/new")
    public String createSchedule(
            @ModelAttribute("scheduleRequest") ScheduleRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model,
            RedirectAttributes redirectAttributes) {

        String userRole = userDetails.getEmployee().getRole().getRoleCode().name();
        Integer managerBranchId = userDetails.getEmployee().getBranch() != null ?
                userDetails.getEmployee().getBranch().getBranchId() : null;

        if ("MANAGER".equals(userRole)) {
            request.setBranchId(managerBranchId);
        }

        try {
            scheduleService.create(request);
            redirectAttributes.addFlashAttribute("success", "Thêm lịch làm việc thành công!");
            return "redirect:/manager/schedule";
        } catch (ValidationException e) {
            model.addAttribute("error", e.getMessage());
            populateFormOptions(model, userRole, managerBranchId);
            model.addAttribute("scheduleRequest", request);
            model.addAttribute("isEdit", false);
            return "manager/schedule/schedule-form";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model,
            RedirectAttributes redirectAttributes) {

        EmployeeShiftSchedule detail = scheduleService.getDetail(id);
        String userRole = userDetails.getEmployee().getRole().getRoleCode().name();
        Integer managerBranchId = userDetails.getEmployee().getBranch() != null ?
                userDetails.getEmployee().getBranch().getBranchId() : null;

        if ("MANAGER".equals(userRole) && !detail.getBranch().getBranchId().equals(managerBranchId)) {
            redirectAttributes.addFlashAttribute("error", "Từ chối quyền truy cập: Lịch làm việc không thuộc chi nhánh của bạn.");
            return "redirect:/manager/schedule";
        }

        ScheduleRequest request = ScheduleRequest.builder()
                .employeeId(detail.getEmployee().getEmployeeId())
                .branchId(detail.getBranch().getBranchId())
                .shiftTypeId(detail.getShiftType().getShiftTypeId())
                .dayOfWeek(detail.getDayOfWeek())
                .effectiveFrom(detail.getEffectiveFrom())
                .effectiveTo(detail.getEffectiveTo())
                .build();

        populateFormOptions(model, userRole, managerBranchId);
        model.addAttribute("scheduleRequest", request);
        model.addAttribute("isEdit", true);
        model.addAttribute("scheduleId", id);
        return "manager/schedule/schedule-form";
    }

    @PostMapping("/{id}/edit")
    public String updateSchedule(
            @PathVariable("id") Long id,
            @ModelAttribute("scheduleRequest") ScheduleRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model,
            RedirectAttributes redirectAttributes) {

        EmployeeShiftSchedule detail = scheduleService.getDetail(id);
        String userRole = userDetails.getEmployee().getRole().getRoleCode().name();
        Integer managerBranchId = userDetails.getEmployee().getBranch() != null ?
                userDetails.getEmployee().getBranch().getBranchId() : null;

        if ("MANAGER".equals(userRole) && !detail.getBranch().getBranchId().equals(managerBranchId)) {
            redirectAttributes.addFlashAttribute("error", "Từ chối quyền truy cập: Lịch làm việc không thuộc chi nhánh của bạn.");
            return "redirect:/manager/schedule";
        }

        if ("MANAGER".equals(userRole)) {
            request.setBranchId(managerBranchId);
        }

        try {
            scheduleService.update(id, request);
            redirectAttributes.addFlashAttribute("success", "Cập nhật lịch làm việc thành công!");
            return "redirect:/manager/schedule";
        } catch (ValidationException e) {
            model.addAttribute("error", e.getMessage());
            populateFormOptions(model, userRole, managerBranchId);
            model.addAttribute("scheduleRequest", request);
            model.addAttribute("isEdit", true);
            model.addAttribute("scheduleId", id);
            return "manager/schedule/schedule-form";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteSchedule(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        EmployeeShiftSchedule detail = scheduleService.getDetail(id);
        String userRole = userDetails.getEmployee().getRole().getRoleCode().name();
        Integer managerBranchId = userDetails.getEmployee().getBranch() != null ?
                userDetails.getEmployee().getBranch().getBranchId() : null;

        if ("MANAGER".equals(userRole) && !detail.getBranch().getBranchId().equals(managerBranchId)) {
            redirectAttributes.addFlashAttribute("error", "Từ chối quyền truy cập: Lịch làm việc không thuộc chi nhánh của bạn.");
            return "redirect:/manager/schedule";
        }

        try {
            scheduleService.delete(id);
            redirectAttributes.addFlashAttribute("success", "Xóa lịch làm việc thành công!");
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/manager/schedule";
    }

    private void populateFormOptions(Model model, String userRole, Integer managerBranchId) {
        if ("MANAGER".equals(userRole)) {
            model.addAttribute("branches", branchRepository.findById(managerBranchId).stream().collect(Collectors.toList()));
            model.addAttribute("employees", employeeRepository.findAll().stream()
                    .filter(e -> e.getBranch() != null && e.getBranch().getBranchId().equals(managerBranchId))
                    .collect(Collectors.toList()));
        } else {
            model.addAttribute("branches", branchRepository.findAll());
            model.addAttribute("employees", employeeRepository.findAll());
        }
        model.addAttribute("shiftTypes", shiftTypeRepository.findAll());
    }
}

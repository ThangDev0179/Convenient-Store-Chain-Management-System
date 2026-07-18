package com.retail.controller;

import com.retail.dto.*;
import com.retail.entity.Branch;
import com.retail.entity.EmployeeStatus;
import com.retail.entity.Role;
import com.retail.entity.RoleCode;
import com.retail.exception.BranchAlreadyHasManagerException;
import com.retail.exception.ValidationException;
import com.retail.repository.BranchRepository;
import com.retail.repository.RoleRepository;
import com.retail.security.CustomUserDetails;
import com.retail.service.EmployeeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/employees")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private RoleRepository roleRepository;

    @GetMapping
    public String listEmployees(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "branchId", required = false) Integer branchId,
            @RequestParam(value = "roleId", required = false) Long roleId,
            @RequestParam(value = "status", required = false) EmployeeStatus status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5") int size,
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "direction", defaultValue = "DESC") String direction,
            @AuthenticationPrincipal CustomUserDetails authenticatedUser,
            Model model) {

        String userRole = authenticatedUser.getEmployee().getRole().getRoleCode().name();
        Integer managerBranchId = authenticatedUser.getEmployee().getBranch() != null ?
                authenticatedUser.getEmployee().getBranch().getBranchId() : null;

        // Multi-tenancy: Manager can only view employees in their own branch
        if ("MANAGER".equals(userRole)) {
            branchId = managerBranchId;
            model.addAttribute("isManagerUser", true);
        } else {
            model.addAttribute("isManagerUser", false);
        }

        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<EmployeeResponse> employeePage = employeeService.list(search, branchId, roleId, status, pageable);

        model.addAttribute("employees", employeePage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", employeePage.getTotalPages());
        model.addAttribute("totalItems", employeePage.getTotalElements());

        // Filters
        model.addAttribute("search", search);
        model.addAttribute("branchId", branchId);
        model.addAttribute("roleId", roleId);
        model.addAttribute("status", status);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("direction", direction);

        // Options
        if ("ADMIN".equals(userRole)) {
            model.addAttribute("branches", branchRepository.findAll());
            model.addAttribute("roles", roleRepository.findAll());
        } else {
            model.addAttribute("branches", branchRepository.findById(managerBranchId).stream().collect(Collectors.toList()));
            model.addAttribute("roles", roleRepository.findAll().stream()
                    .filter(r -> r.getRoleCode() != RoleCode.ADMIN)
                    .collect(Collectors.toList()));
        }

        return "admin/employees/employee-list";
    }

    @GetMapping("/new")
    public String showCreateForm(@AuthenticationPrincipal CustomUserDetails authenticatedUser, Model model) {
        String userRole = authenticatedUser.getEmployee().getRole().getRoleCode().name();
        Integer managerBranchId = authenticatedUser.getEmployee().getBranch() != null ?
                authenticatedUser.getEmployee().getBranch().getBranchId() : null;

        CreateEmployeeRequest request = new CreateEmployeeRequest();
        if ("MANAGER".equals(userRole)) {
            request.setBranchId(managerBranchId);
            model.addAttribute("branches", branchRepository.findById(managerBranchId).stream().collect(Collectors.toList()));
            model.addAttribute("roles", roleRepository.findAll().stream()
                    .filter(r -> r.getRoleCode() != RoleCode.ADMIN)
                    .collect(Collectors.toList()));
        } else {
            model.addAttribute("branches", branchRepository.findAll());
            model.addAttribute("roles", roleRepository.findAll());
        }

        model.addAttribute("employeeRequest", request);
        model.addAttribute("isEdit", false);
        return "admin/employees/employee-form";
    }

    @PostMapping("/new")
    public String createEmployee(
            @ModelAttribute("employeeRequest") CreateEmployeeRequest request,
            @AuthenticationPrincipal CustomUserDetails authenticatedUser,
            Model model,
            RedirectAttributes redirectAttributes) {

        String userRole = authenticatedUser.getEmployee().getRole().getRoleCode().name();
        Integer managerBranchId = authenticatedUser.getEmployee().getBranch() != null ?
                authenticatedUser.getEmployee().getBranch().getBranchId() : null;

        if ("MANAGER".equals(userRole)) {
            request.setBranchId(managerBranchId);
            Role requestedRole = roleRepository.findById(request.getRoleId()).orElse(null);
            if (requestedRole != null && requestedRole.getRoleCode() == RoleCode.ADMIN) {
                model.addAttribute("error", "Quản lý không có quyền tạo tài khoản Quản trị viên (Admin).");
                populateFormModels(model, userRole, managerBranchId);
                model.addAttribute("employeeRequest", request);
                model.addAttribute("isEdit", false);
                return "admin/employees/employee-form";
            }
        }

        try {
            employeeService.create(request);
            redirectAttributes.addFlashAttribute("success", "Tạo mới nhân viên thành công! Thông tin đăng nhập đã được gửi đến email.");
            return "redirect:/admin/employees";
        } catch (BranchAlreadyHasManagerException | ValidationException e) {
            model.addAttribute("error", e.getMessage());
            populateFormModels(model, userRole, managerBranchId);
            model.addAttribute("employeeRequest", request);
            model.addAttribute("isEdit", false);
            return "admin/employees/employee-form";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CustomUserDetails authenticatedUser,
            Model model,
            RedirectAttributes redirectAttributes) {

        EmployeeResponse detail = employeeService.getDetail(id);
        String userRole = authenticatedUser.getEmployee().getRole().getRoleCode().name();
        Integer managerBranchId = authenticatedUser.getEmployee().getBranch() != null ?
                authenticatedUser.getEmployee().getBranch().getBranchId() : null;

        // Security check
        if ("MANAGER".equals(userRole) && !detail.getBranchId().equals(managerBranchId)) {
            redirectAttributes.addFlashAttribute("error", "Từ chối quyền truy cập: Nhân viên không thuộc chi nhánh của bạn.");
            return "redirect:/admin/employees";
        }

        UpdateEmployeeRequest request = UpdateEmployeeRequest.builder()
                .fullName(detail.getFullName())
                .email(detail.getEmail())
                .phone(detail.getPhone())
                .build();

        model.addAttribute("employeeRequest", request);
        model.addAttribute("employeeCode", detail.getEmployeeCode());
        model.addAttribute("username", detail.getUsername());
        model.addAttribute("isEdit", true);
        model.addAttribute("employeeId", id);
        return "admin/employees/employee-form";
    }

    @PostMapping("/{id}/edit")
    public String updateEmployee(
            @PathVariable("id") Long id,
            @ModelAttribute("employeeRequest") UpdateEmployeeRequest request,
            @AuthenticationPrincipal CustomUserDetails authenticatedUser,
            Model model,
            RedirectAttributes redirectAttributes) {

        EmployeeResponse detail = employeeService.getDetail(id);
        String userRole = authenticatedUser.getEmployee().getRole().getRoleCode().name();
        Integer managerBranchId = authenticatedUser.getEmployee().getBranch() != null ?
                authenticatedUser.getEmployee().getBranch().getBranchId() : null;

        // Security check
        if ("MANAGER".equals(userRole) && !detail.getBranchId().equals(managerBranchId)) {
            redirectAttributes.addFlashAttribute("error", "Từ chối quyền truy cập: Nhân viên không thuộc chi nhánh của bạn.");
            return "redirect:/admin/employees";
        }

        try {
            employeeService.update(id, request);
            redirectAttributes.addFlashAttribute("success", "Cập nhật thông tin nhân viên thành công!");
            return "redirect:/admin/employees";
        } catch (ValidationException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("employeeRequest", request);
            model.addAttribute("employeeCode", detail.getEmployeeCode());
            model.addAttribute("username", detail.getUsername());
            model.addAttribute("isEdit", true);
            model.addAttribute("employeeId", id);
            return "admin/employees/employee-form";
        }
    }

    @PostMapping("/{id}/lock")
    public String toggleLock(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CustomUserDetails authenticatedUser,
            RedirectAttributes redirectAttributes) {

        EmployeeResponse detail = employeeService.getDetail(id);
        String userRole = authenticatedUser.getEmployee().getRole().getRoleCode().name();
        Integer managerBranchId = authenticatedUser.getEmployee().getBranch() != null ?
                authenticatedUser.getEmployee().getBranch().getBranchId() : null;

        // Security check
        if ("MANAGER".equals(userRole) && !detail.getBranchId().equals(managerBranchId)) {
            redirectAttributes.addFlashAttribute("error", "Từ chối quyền truy cập: Nhân viên không thuộc chi nhánh của bạn.");
            return "redirect:/admin/employees";
        }

        try {
            if (detail.getStatus() == EmployeeStatus.Active) {
                employeeService.lock(id);
                redirectAttributes.addFlashAttribute("success", "Đã khóa tài khoản nhân viên!");
            } else {
                employeeService.unlock(id);
                redirectAttributes.addFlashAttribute("success", "Đã kích hoạt lại tài khoản nhân viên!");
            }
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/employees";
    }

    @PostMapping("/{id}/reset-password")
    public String resetPassword(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CustomUserDetails authenticatedUser,
            RedirectAttributes redirectAttributes) {

        EmployeeResponse detail = employeeService.getDetail(id);
        String userRole = authenticatedUser.getEmployee().getRole().getRoleCode().name();
        Integer managerBranchId = authenticatedUser.getEmployee().getBranch() != null ?
                authenticatedUser.getEmployee().getBranch().getBranchId() : null;

        // Security check
        if ("MANAGER".equals(userRole) && !detail.getBranchId().equals(managerBranchId)) {
            redirectAttributes.addFlashAttribute("error", "Từ chối quyền truy cập: Nhân viên không thuộc chi nhánh của bạn.");
            return "redirect:/admin/employees";
        }

        employeeService.resetPassword(id);
        redirectAttributes.addFlashAttribute("success", "Đã đặt lại mật khẩu! Mật khẩu mới đã được ghi nhận và gửi đến email.");
        return "redirect:/admin/employees";
    }

    @PostMapping("/{id}/change-role")
    public String changeRole(
            @PathVariable("id") Long id,
            @RequestParam("roleId") Long roleId,
            @AuthenticationPrincipal CustomUserDetails authenticatedUser,
            RedirectAttributes redirectAttributes) {

        String userRole = authenticatedUser.getEmployee().getRole().getRoleCode().name();

        // ONLY Admin can change roles
        if (!"ADMIN".equals(userRole)) {
            redirectAttributes.addFlashAttribute("error", "Từ chối quyền thực hiện: Chỉ có Quản trị viên mới được phép đổi chức vụ nhân viên.");
            return "redirect:/admin/employees";
        }

        try {
            ChangeRoleRequest request = new ChangeRoleRequest(roleId);
            employeeService.changeRole(id, request);
            redirectAttributes.addFlashAttribute("success", "Đổi chức vụ thành công!");
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/employees";
    }

    @PostMapping("/{id}/transfer")
    public String transferBranch(
            @PathVariable("id") Long id,
            @RequestParam("newBranchId") Integer newBranchId,
            @AuthenticationPrincipal CustomUserDetails authenticatedUser,
            RedirectAttributes redirectAttributes) {

        String userRole = authenticatedUser.getEmployee().getRole().getRoleCode().name();

        // ONLY Admin can transfer branches
        if (!"ADMIN".equals(userRole)) {
            redirectAttributes.addFlashAttribute("error", "Từ chối quyền thực hiện: Chỉ có Quản trị viên mới được phép luân chuyển chi nhánh.");
            return "redirect:/admin/employees";
        }

        try {
            TransferBranchRequest request = new TransferBranchRequest(newBranchId);
            employeeService.transferBranch(id, request);
            redirectAttributes.addFlashAttribute("success", "Luân chuyển chi nhánh thành công!");
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/employees";
    }

    @GetMapping("/{id}")
    public String getEmployeeDetail(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CustomUserDetails authenticatedUser,
            Model model,
            RedirectAttributes redirectAttributes) {

        EmployeeResponse detail = employeeService.getDetail(id);
        String userRole = authenticatedUser.getEmployee().getRole().getRoleCode().name();
        Integer managerBranchId = authenticatedUser.getEmployee().getBranch() != null ?
                authenticatedUser.getEmployee().getBranch().getBranchId() : null;

        // Security check
        if ("MANAGER".equals(userRole) && !detail.getBranchId().equals(managerBranchId)) {
            redirectAttributes.addFlashAttribute("error", "Từ chối quyền truy cập: Nhân viên không thuộc chi nhánh của bạn.");
            return "redirect:/admin/employees";
        }

        model.addAttribute("employee", detail);
        model.addAttribute("roles", roleRepository.findAll());
        model.addAttribute("branches", branchRepository.findAll());
        model.addAttribute("currentUserRole", userRole);

        return "admin/employees/employee-detail";
    }

    private void populateFormModels(Model model, String userRole, Integer managerBranchId) {
        if ("MANAGER".equals(userRole)) {
            model.addAttribute("branches", branchRepository.findById(managerBranchId).stream().collect(Collectors.toList()));
            model.addAttribute("roles", roleRepository.findAll().stream()
                    .filter(r -> r.getRoleCode() != RoleCode.ADMIN)
                    .collect(Collectors.toList()));
        } else {
            model.addAttribute("branches", branchRepository.findAll());
            model.addAttribute("roles", roleRepository.findAll());
        }
    }
}

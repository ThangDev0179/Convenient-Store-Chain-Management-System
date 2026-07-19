package com.retail.controller;
import com.retail.exception.BranchHasActiveDataException;
import com.retail.dto.BranchResponse;
import com.retail.service.BranchService;
import com.retail.entity.BranchStatus;
import com.retail.dto.CreateBranchRequest;
import com.retail.exception.DuplicateBranchCodeException;
import com.retail.dto.UpdateBranchRequest;
import com.retail.exception.ValidationException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/branches")
public class BranchController {

    @Autowired
    private BranchService branchService;

    @GetMapping
    public String listBranches(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "status", required = false) BranchStatus status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5") int size,
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "direction", defaultValue = "DESC") String direction,
            Model model) {

        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<BranchResponse> branchPage = branchService.list(search, status, pageable);

        model.addAttribute("branches", branchPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", branchPage.getTotalPages());
        model.addAttribute("totalItems", branchPage.getTotalElements());
        model.addAttribute("search", search);
        model.addAttribute("status", status);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("direction", direction);

        return "admin/branches/branch-list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("branchRequest", new CreateBranchRequest());
        model.addAttribute("isEdit", false);
        return "admin/branches/branch-form";
    }

    @PostMapping("/new")
    public String createBranch(@ModelAttribute("branchRequest") CreateBranchRequest request, Model model, RedirectAttributes redirectAttributes) {
        try {
            branchService.create(request);
            redirectAttributes.addFlashAttribute("success", "Tạo mới chi nhánh thành công!");
            return "redirect:/admin/branches";
        } catch (DuplicateBranchCodeException | ValidationException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("branchRequest", request);
            model.addAttribute("isEdit", false);
            return "admin/branches/branch-form";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable("id") Integer id, Model model) {
        BranchResponse detail = branchService.getDetail(id);
        UpdateBranchRequest request = UpdateBranchRequest.builder()
                .branchName(detail.getBranchName())
                .address(detail.getAddress())
                .build();

        model.addAttribute("branchRequest", request);
        model.addAttribute("branchCode", detail.getBranchCode());
        model.addAttribute("isEdit", true);
        model.addAttribute("branchId", id);
        return "admin/branches/branch-form";
    }

    @PostMapping("/{id}/edit")
    public String updateBranch(
            @PathVariable("id") Integer id,
            @ModelAttribute("branchRequest") UpdateBranchRequest request,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            branchService.update(id, request);
            redirectAttributes.addFlashAttribute("success", "Cập nhật chi nhánh thành công!");
            return "redirect:/admin/branches";
        } catch (ValidationException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("branchRequest", request);
            model.addAttribute("branchCode", branchService.getDetail(id).getBranchCode());
            model.addAttribute("isEdit", true);
            model.addAttribute("branchId", id);
            return "admin/branches/branch-form";
        }
    }

    @PostMapping("/{id}/archive")
    public String archiveBranch(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            branchService.archive(id);
            redirectAttributes.addFlashAttribute("success", "Lưu trữ chi nhánh thành công!");
        } catch (BranchHasActiveDataException | ValidationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/branches";
    }

    @PostMapping("/{id}/close")
    public String toggleCloseBranch(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            BranchResponse detail = branchService.getDetail(id);
            if (detail.getStatus() == BranchStatus.Active) {
                branchService.close(id);
                redirectAttributes.addFlashAttribute("success", "Đã đóng cửa chi nhánh thành công!");
            } else {
                branchService.reopen(id);
                redirectAttributes.addFlashAttribute("success", "Đã mở lại chi nhánh thành công!");
            }
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/branches";
    }

    @GetMapping("/{id}")
    public String getBranchDetail(@PathVariable("id") Integer id, Model model) {
        BranchResponse detail = branchService.getDetail(id);
        model.addAttribute("branch", detail);
        return "admin/branches/branch-detail";
    }
}
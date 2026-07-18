package com.retail.report.controller;

import com.retail.report.dto.LossReportDto;
import com.retail.report.dto.StockValueReportDto;
import com.retail.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public String dashboard(Model model) {
        Map<String, Object> metrics = reportService.getDashboardMetrics();
        model.addAttribute("metrics", metrics);
        
        // Lấy báo cáo tồn kho để vẽ biểu đồ
        List<StockValueReportDto> stockReport = reportService.getStockValueReport(null);
        model.addAttribute("stockReport", stockReport);
        
        return "report/dashboard";
    }

    @GetMapping("/stock")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public String stockReport(@RequestParam(required = false) Integer branchId, Model model) {
        List<StockValueReportDto> report = reportService.getStockValueReport(branchId);
        model.addAttribute("report", report);
        model.addAttribute("selectedBranchId", branchId);
        return "report/stock-report";
    }

    @GetMapping("/loss")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public String lossReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model) {
            
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime end = endDate != null ? endDate.atTime(LocalTime.MAX) : null;
        
        List<LossReportDto> report = reportService.getLossReport(start, end);
        model.addAttribute("report", report);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        return "report/loss-report";
    }
}

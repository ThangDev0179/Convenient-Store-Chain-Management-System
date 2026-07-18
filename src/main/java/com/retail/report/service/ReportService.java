package com.retail.report.service;

import com.retail.report.dto.LossReportDto;
import com.retail.report.dto.StockValueReportDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface ReportService {
    List<StockValueReportDto> getStockValueReport(Integer branchId);
    
    List<LossReportDto> getLossReport(LocalDateTime startDate, LocalDateTime endDate);
    
    Map<String, Object> getDashboardMetrics();
}

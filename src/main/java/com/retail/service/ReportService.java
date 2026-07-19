package com.retail.service;

import com.retail.dto.LossReportDto;
import com.retail.dto.StockValueReportDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface ReportService {
    List<StockValueReportDto> getStockValueReport(Integer branchId);
    
    List<LossReportDto> getLossReport(LocalDateTime startDate, LocalDateTime endDate);
    
    Map<String, Object> getDashboardMetrics();
}

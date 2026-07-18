package com.retail.report.service.impl;

import com.retail.disposal.entity.DisposalSourceType;
import com.retail.report.dto.LossReportDto;
import com.retail.report.dto.StockValueReportDto;
import com.retail.report.service.ReportService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final EntityManager entityManager;

    @Override
    @SuppressWarnings("unchecked")
    public List<StockValueReportDto> getStockValueReport(Integer branchId) {
        StringBuilder jpql = new StringBuilder(
                "SELECT b.branchId, b.branchName, SUM(bi.qtyOnHand), SUM(bi.qtyAvailable), SUM(bi.qtyOnHand * p.standardPrice) " +
                "FROM BranchInventory bi " +
                "JOIN bi.branch b " +
                "JOIN bi.product p "
        );
        
        if (branchId != null) {
            jpql.append("WHERE b.branchId = :branchId ");
        }
        
        jpql.append("GROUP BY b.branchId, b.branchName ORDER BY b.branchId");
        
        Query query = entityManager.createQuery(jpql.toString());
        if (branchId != null) {
            query.setParameter("branchId", branchId);
        }
        
        List<Object[]> results = query.getResultList();
        List<StockValueReportDto> dtos = new ArrayList<>();
        
        for (Object[] row : results) {
            StockValueReportDto dto = new StockValueReportDto();
            dto.setBranchId((Integer) row[0]);
            dto.setBranchName((String) row[1]);
            dto.setTotalQtyOnHand((BigDecimal) row[2]);
            dto.setTotalQtyAvailable((BigDecimal) row[3]);
            dto.setTotalValue((BigDecimal) row[4]);
            dtos.add(dto);
        }
        
        return dtos;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<LossReportDto> getLossReport(LocalDateTime startDate, LocalDateTime endDate) {
        StringBuilder jpql = new StringBuilder(
                "SELECT b.branchId, b.branchName, d.sourceType, SUM(dd.quantityDisposed), SUM(dd.quantityDisposed * dd.unitCost) " +
                "FROM StockDisposalDetail dd " +
                "JOIN dd.stockDisposal d " +
                "JOIN d.branch b " +
                "WHERE d.status = 'Approved' "
        );
        
        if (startDate != null) {
            jpql.append("AND d.approvedAt >= :startDate ");
        }
        if (endDate != null) {
            jpql.append("AND d.approvedAt <= :endDate ");
        }
        
        jpql.append("GROUP BY b.branchId, b.branchName, d.sourceType ORDER BY b.branchId, d.sourceType");
        
        Query query = entityManager.createQuery(jpql.toString());
        if (startDate != null) {
            query.setParameter("startDate", startDate);
        }
        if (endDate != null) {
            query.setParameter("endDate", endDate);
        }
        
        List<Object[]> results = query.getResultList();
        List<LossReportDto> dtos = new ArrayList<>();
        
        for (Object[] row : results) {
            LossReportDto dto = new LossReportDto();
            dto.setBranchId((Integer) row[0]);
            dto.setBranchName((String) row[1]);
            dto.setSourceType((DisposalSourceType) row[2]);
            dto.setTotalDisposedQty((BigDecimal) row[3]);
            dto.setTotalLossValue((BigDecimal) row[4]);
            dtos.add(dto);
        }
        
        return dtos;
    }

    @Override
    public Map<String, Object> getDashboardMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        // 1. Tổng giá trị tồn kho toàn hệ thống
        String stockJpql = "SELECT SUM(bi.qtyOnHand * p.standardPrice) FROM BranchInventory bi JOIN bi.product p";
        BigDecimal totalStockValue = (BigDecimal) entityManager.createQuery(stockJpql).getSingleResult();
        metrics.put("totalStockValue", totalStockValue != null ? totalStockValue : BigDecimal.ZERO);
        
        // 2. Tổng giá trị thất thoát (Approved) trong tháng hiện tại
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        String lossJpql = "SELECT SUM(dd.quantityDisposed * dd.unitCost) FROM StockDisposalDetail dd JOIN dd.stockDisposal d " +
                          "WHERE d.status = 'Approved' AND d.approvedAt >= :startOfMonth";
        BigDecimal totalLossValue = (BigDecimal) entityManager.createQuery(lossJpql)
                                            .setParameter("startOfMonth", startOfMonth)
                                            .getSingleResult();
        metrics.put("totalLossValue", totalLossValue != null ? totalLossValue : BigDecimal.ZERO);
        
        // 3. Số lượng phiếu xuất hủy nháp (Draft)
        String draftDisposalJpql = "SELECT COUNT(d) FROM StockDisposal d WHERE d.status = 'Draft'";
        Long draftDisposals = (Long) entityManager.createQuery(draftDisposalJpql).getSingleResult();
        metrics.put("draftDisposals", draftDisposals);
        
        // 4. Số lượng phiếu kiểm kê chờ duyệt (Submitted)
        String pendingCountJpql = "SELECT COUNT(c) FROM InventoryCount c WHERE c.status = 'Submitted'";
        Long pendingCounts = (Long) entityManager.createQuery(pendingCountJpql).getSingleResult();
        metrics.put("pendingCounts", pendingCounts);
        
        // 5. Số lượng phiếu điều chuyển đang vận chuyển (In_Transit)
        String transitTransferJpql = "SELECT COUNT(t) FROM StockTransfer t WHERE t.status = 'In_Transit'";
        Long transitTransfers = (Long) entityManager.createQuery(transitTransferJpql).getSingleResult();
        metrics.put("transitTransfers", transitTransfers);

        return metrics;
    }
}

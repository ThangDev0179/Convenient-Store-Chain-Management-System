package com.retail.service.impl;

import com.retail.dto.BranchInventoryResponse;
import com.retail.dto.InventoryTransactionResponse;
import com.retail.entity.BranchInventory;
import com.retail.entity.InventoryTransactionHistory;
import com.retail.entity.ProductUOM;
import com.retail.repository.BranchInventoryRepository;
import com.retail.repository.InventoryTransactionHistoryRepository;
import com.retail.repository.ProductUOMRepository;
import com.retail.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class InventoryServiceImpl implements InventoryService {

    @Autowired
    private BranchInventoryRepository branchInventoryRepository;

    @Autowired
    private InventoryTransactionHistoryRepository inventoryTransactionHistoryRepository;

    @Autowired
    private ProductUOMRepository productUOMRepository;

    @Override
    public Page<BranchInventoryResponse> searchInventory(Integer branchId, String keyword, Integer categoryId, Pageable pageable) {
        Page<BranchInventory> inventoryPage = branchInventoryRepository.searchInventory(branchId, keyword, categoryId, pageable);
        return inventoryPage.map(this::mapToInventoryResponse);
    }

    @Override
    public Page<InventoryTransactionResponse> filterHistory(Integer branchId, Long productId, LocalDateTime startDate, LocalDateTime endDate, String transactionType, Pageable pageable) {
        // Normalize transactionType empty value to null
        String txType = (transactionType == null || transactionType.trim().isEmpty()) ? null : transactionType.trim();
        Page<InventoryTransactionHistory> historyPage = inventoryTransactionHistoryRepository.filterHistory(branchId, productId, startDate, endDate, txType, pageable);
        return historyPage.map(this::mapToTransactionResponse);
    }

    private String formatQuantity(BigDecimal qty, List<ProductUOM> uoms) {
        if (qty == null) return "0";
        if (uoms == null || uoms.isEmpty()) {
            return qty.stripTrailingZeros().toPlainString();
        }

        // Sort by conversion rate descending
        List<ProductUOM> sortedUoms = uoms.stream()
                .sorted((u1, u2) -> u2.getConversionRate().compareTo(u1.getConversionRate()))
                .toList();

        StringBuilder sb = new StringBuilder();
        BigDecimal remaining = qty.abs(); // Keep positive for division, add sign at the end
        boolean isNegative = qty.compareTo(BigDecimal.ZERO) < 0;

        for (ProductUOM uom : sortedUoms) {
            BigDecimal rate = BigDecimal.valueOf(uom.getConversionRate());
            if (rate.compareTo(BigDecimal.ONE) > 0) {
                BigDecimal[] divideAndRemainder = remaining.divideAndRemainder(rate);
                BigDecimal quotient = divideAndRemainder[0];
                if (quotient.compareTo(BigDecimal.ZERO) > 0) {
                    if (sb.length() > 0) {
                        sb.append(", ");
                    }
                    sb.append(quotient.stripTrailingZeros().toPlainString()).append(" ").append(uom.getUomName());
                    remaining = divideAndRemainder[1];
                }
            } else {
                // Base unit
                if (remaining.compareTo(BigDecimal.ZERO) > 0 || sb.length() == 0) {
                    if (sb.length() > 0) {
                        sb.append(", ");
                    }
                    sb.append(remaining.stripTrailingZeros().toPlainString()).append(" ").append(uom.getUomName());
                    remaining = BigDecimal.ZERO;
                }
            }
        }

        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(remaining.stripTrailingZeros().toPlainString());
        }

        return (isNegative ? "-" : "") + sb.toString();
    }

    private BranchInventoryResponse mapToInventoryResponse(BranchInventory bi) {
        List<ProductUOM> uoms = productUOMRepository.findByProductProductId(bi.getProduct().getProductId());
        String baseUom = uoms.stream()
                .filter(ProductUOM::getIsBaseUnit)
                .map(ProductUOM::getUomName)
                .findFirst()
                .orElse("Đơn vị");

        return BranchInventoryResponse.builder()
                .branchId(bi.getBranch().getBranchId())
                .branchName(bi.getBranch().getBranchName())
                .productId(bi.getProduct().getProductId())
                .productSku(bi.getProduct().getSku())
                .productName(bi.getProduct().getProductName())
                .categoryName(bi.getProduct().getCategory() != null ? bi.getProduct().getCategory().getCategoryName() : "N/A")
                .qtyOnHand(bi.getQtyOnHand())
                .qtyAvailable(bi.getQtyAvailable())
                .qtyInTransit(bi.getQtyInTransit())
                .baseUomName(baseUom)
                .qtyOnHandFormatted(formatQuantity(bi.getQtyOnHand(), uoms))
                .qtyAvailableFormatted(formatQuantity(bi.getQtyAvailable(), uoms))
                .qtyInTransitFormatted(formatQuantity(bi.getQtyInTransit(), uoms))
                .updatedAt(bi.getUpdatedAt())
                .build();
    }

    private InventoryTransactionResponse mapToTransactionResponse(InventoryTransactionHistory ith) {
        List<ProductUOM> uoms = productUOMRepository.findByProductProductId(ith.getProduct().getProductId());
        String baseUom = uoms.stream()
                .filter(ProductUOM::getIsBaseUnit)
                .map(ProductUOM::getUomName)
                .findFirst()
                .orElse("Đơn vị");

        return InventoryTransactionResponse.builder()
                .transactionId(ith.getTransactionId())
                .branchId(ith.getBranch().getBranchId())
                .branchName(ith.getBranch().getBranchName())
                .productId(ith.getProduct().getProductId())
                .productSku(ith.getProduct().getSku())
                .productName(ith.getProduct().getProductName())
                .transactionType(ith.getTransactionType().name())
                .referenceTable(ith.getReferenceTable())
                .referenceId(ith.getReferenceId())
                .quantityChange(ith.getQuantityChange())
                .baseUomName(baseUom)
                .quantityChangeFormatted(formatQuantity(ith.getQuantityChange(), uoms))
                .reason(ith.getReason())
                .createdByName(ith.getCreatedBy() != null ? ith.getCreatedBy().getFullName() : "System")
                .createdAt(ith.getCreatedAt())
                .build();
    }
}


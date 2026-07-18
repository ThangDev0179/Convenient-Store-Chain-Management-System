package com.retail.service.impl;

import com.retail.dto.BranchInventoryResponse;
import com.retail.dto.InventoryTransactionResponse;
import com.retail.entity.BranchInventory;
import com.retail.entity.InventoryTransactionHistory;
import com.retail.repository.BranchInventoryRepository;
import com.retail.repository.InventoryTransactionHistoryRepository;
import com.retail.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
public class InventoryServiceImpl implements InventoryService {

    @Autowired
    private BranchInventoryRepository branchInventoryRepository;

    @Autowired
    private InventoryTransactionHistoryRepository inventoryTransactionHistoryRepository;

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

    private BranchInventoryResponse mapToInventoryResponse(BranchInventory bi) {
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
                .updatedAt(bi.getUpdatedAt())
                .build();
    }

    private InventoryTransactionResponse mapToTransactionResponse(InventoryTransactionHistory ith) {
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
                .reason(ith.getReason())
                .createdByName(ith.getCreatedBy() != null ? ith.getCreatedBy().getFullName() : "System")
                .createdAt(ith.getCreatedAt())
                .build();
    }
}

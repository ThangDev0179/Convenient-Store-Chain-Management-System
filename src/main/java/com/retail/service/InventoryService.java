package com.retail.service;

import com.retail.dto.BranchInventoryResponse;
import com.retail.dto.InventoryTransactionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;

public interface InventoryService {

    Page<BranchInventoryResponse> searchInventory(Integer branchId, String keyword, Integer categoryId, Pageable pageable);

    Page<InventoryTransactionResponse> filterHistory(Integer branchId, Long productId, LocalDateTime startDate, LocalDateTime endDate, String transactionType, Pageable pageable);
}

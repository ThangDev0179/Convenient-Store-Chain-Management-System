package com.retail.service;
import com.retail.entity.InventoryCount;
import com.retail.dto.InventoryCountRequest;


import java.util.List;

public interface InventoryCountService {
    InventoryCount createDraftCount(InventoryCountRequest request, Long createdByEmployeeId);
    InventoryCount submitCount(Long countId, Long employeeId);
    InventoryCount approveCount(Long countId, Long approvedByEmployeeId);
    InventoryCount rejectCount(Long countId, Long rejectedByEmployeeId);
    List<InventoryCount> getAllCounts();
    InventoryCount getCountById(Long countId);
}
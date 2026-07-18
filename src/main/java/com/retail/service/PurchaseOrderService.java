package com.retail.service;

import com.retail.dto.CreatePurchaseOrderRequest;
import com.retail.dto.PurchaseOrderResponse;
import com.retail.entity.Employee;
import org.springframework.data.domain.Page;

public interface PurchaseOrderService {
    PurchaseOrderResponse createPurchaseOrder(CreatePurchaseOrderRequest request, Employee creator);
    PurchaseOrderResponse submitPurchaseOrder(Long poId, Employee user);
    PurchaseOrderResponse cancelPurchaseOrder(Long poId, Employee user);
    PurchaseOrderResponse getPurchaseOrderById(Long poId);
    Page<PurchaseOrderResponse> searchPurchaseOrders(Integer branchId, String statusStr, String keyword, int page, int size);
}

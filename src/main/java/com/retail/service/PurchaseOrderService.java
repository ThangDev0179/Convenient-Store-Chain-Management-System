package com.retail.service;
import com.retail.dto.CreatePurchaseOrderRequest;
import com.retail.entity.Employee;
import com.retail.dto.PurchaseOrderResponse;

import org.springframework.data.domain.Page;

public interface PurchaseOrderService {
    PurchaseOrderResponse createPurchaseOrder(CreatePurchaseOrderRequest request, Employee creator);
    PurchaseOrderResponse submitPurchaseOrder(Long poId, Employee user);
    PurchaseOrderResponse approvePurchaseOrder(Long poId, Employee user);
    PurchaseOrderResponse rejectPurchaseOrder(Long poId, Employee user);
    PurchaseOrderResponse cancelPurchaseOrder(Long poId, Employee user);
    PurchaseOrderResponse getPurchaseOrderById(Long poId);
    Page<PurchaseOrderResponse> searchPurchaseOrders(Integer branchId, String statusStr, String keyword, int page, int size);
}
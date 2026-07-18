package com.retail.procurement;

import com.retail.employee.Employee;
import com.retail.procurement.dto.CreatePurchaseOrderRequest;
import com.retail.procurement.dto.PurchaseOrderResponse;
import org.springframework.data.domain.Page;

public interface PurchaseOrderService {
    PurchaseOrderResponse createPurchaseOrder(CreatePurchaseOrderRequest request, Employee creator);
    PurchaseOrderResponse submitPurchaseOrder(Long poId, Employee user);
    PurchaseOrderResponse cancelPurchaseOrder(Long poId, Employee user);
    PurchaseOrderResponse getPurchaseOrderById(Long poId);
    Page<PurchaseOrderResponse> searchPurchaseOrders(Integer branchId, String statusStr, String keyword, int page, int size);
}

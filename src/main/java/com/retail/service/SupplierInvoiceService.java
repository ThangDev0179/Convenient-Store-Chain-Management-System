package com.retail.service;

import com.retail.dto.SupplierInvoiceResponse;
import com.retail.entity.GoodsReceiptNote;
import com.retail.entity.SupplierInvoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SupplierInvoiceService {
    
    SupplierInvoice createInvoiceFromGrn(GoodsReceiptNote grn);
    
    SupplierInvoiceResponse approveInvoice(Long invoiceId, Long employeeId);
    
    SupplierInvoiceResponse getInvoiceById(Long invoiceId);
    
    Page<SupplierInvoiceResponse> searchInvoices(String keyword, Pageable pageable);
}

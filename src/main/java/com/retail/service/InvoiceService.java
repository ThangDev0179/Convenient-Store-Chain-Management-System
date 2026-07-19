package com.retail.service;

import com.retail.entity.Invoice;
import com.retail.entity.InvoiceDetail;
import java.math.BigDecimal;
import java.util.List;

public interface InvoiceService {
    Invoice createDraftInvoice(Integer branchId, Long cashierId);
    Invoice addDetail(Long invoiceId, Long productId, BigDecimal quantity);
    Invoice checkout(Long invoiceId, String paymentMethod);
    Invoice cancel(Long invoiceId);
    Invoice getDetail(Long invoiceId);
    List<Invoice> getInvoicesByBranch(Integer branchId);
}

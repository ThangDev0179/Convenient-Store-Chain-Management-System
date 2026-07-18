package com.retail.service;

import com.retail.dto.CreateSupplierPaymentRequest;
import com.retail.dto.SupplierPaymentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface SupplierPaymentService {

    SupplierPaymentResponse createPayment(CreateSupplierPaymentRequest request, Long employeeId);

    List<SupplierPaymentResponse> getPaymentsByInvoiceId(Long invoiceId);

    Page<SupplierPaymentResponse> searchPayments(String keyword, Pageable pageable);
}

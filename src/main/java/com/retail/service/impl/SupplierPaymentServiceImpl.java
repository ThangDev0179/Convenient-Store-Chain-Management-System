package com.retail.service.impl;

import com.retail.dto.CreateSupplierPaymentRequest;
import com.retail.dto.SupplierPaymentResponse;
import com.retail.entity.*;
import com.retail.exception.ValidationException;
import com.retail.repository.*;
import com.retail.service.SupplierPaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class SupplierPaymentServiceImpl implements SupplierPaymentService {

    @Autowired
    private SupplierPaymentRepository supplierPaymentRepository;

    @Autowired
    private SupplierInvoiceRepository supplierInvoiceRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Override
    public SupplierPaymentResponse createPayment(CreateSupplierPaymentRequest request, Long employeeId) {
        SupplierInvoice invoice = supplierInvoiceRepository.findById(request.getSupplierInvoiceId())
                .orElseThrow(() -> new ValidationException("Không tìm thấy hóa đơn cần thanh toán."));

        if (!"Approved".equals(invoice.getStatus()) && !"Unpaid".equals(invoice.getStatus()) && !"Partially_Paid".equals(invoice.getStatus())) {
            throw new ValidationException("Hóa đơn phải ở trạng thái Đã duyệt hoặc Thanh toán một phần mới được chi tiền.");
        }

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ValidationException("Nhân viên thực hiện chi tiền không tồn tại."));

        BigDecimal newAmountPaid = invoice.getAmountPaid().add(request.getAmountPaid());
        if (newAmountPaid.compareTo(invoice.getAmount()) > 0) {
            BigDecimal maxPayable = invoice.getAmount().subtract(invoice.getAmountPaid());
            throw new ValidationException("Số tiền thanh toán vượt quá số nợ còn lại của hóa đơn. Tối đa có thể trả thêm: " + maxPayable + "đ");
        }

        invoice.setAmountPaid(newAmountPaid);
        if (newAmountPaid.compareTo(invoice.getAmount()) == 0) {
            invoice.setStatus("Paid");
        } else {
            invoice.setStatus("Partially_Paid");
        }
        supplierInvoiceRepository.save(invoice);

        SupplierPayment payment = SupplierPayment.builder()
                .supplierInvoice(invoice)
                .amountPaid(request.getAmountPaid())
                .paymentMethod(request.getPaymentMethod())
                .paidBy(employee)
                .paidAt(LocalDateTime.now())
                .build();
        SupplierPayment saved = supplierPaymentRepository.save(payment);

        // Write AuditLog
        AuditLog audit = AuditLog.builder()
                .employee(employee)
                .actionType("CreateSupplierPayment")
                .entityName("SupplierPayment")
                .entityId(saved.getSupplierPaymentId())
                .oldValue(null)
                .newValue("{\"amountPaid\":" + request.getAmountPaid() + ",\"paymentMethod\":\"" + request.getPaymentMethod() + "\"}")
                .reason("Paid " + request.getAmountPaid() + "đ for invoice ID: " + invoice.getSupplierInvoiceId())
                .build();
        auditLogRepository.save(audit);

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierPaymentResponse> getPaymentsByInvoiceId(Long invoiceId) {
        return supplierPaymentRepository.findBySupplierInvoiceSupplierInvoiceId(invoiceId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SupplierPaymentResponse> searchPayments(String keyword, Pageable pageable) {
        return supplierPaymentRepository.searchPayments(keyword, pageable).map(this::mapToResponse);
    }

    private SupplierPaymentResponse mapToResponse(SupplierPayment p) {
        return SupplierPaymentResponse.builder()
                .supplierPaymentId(p.getSupplierPaymentId())
                .supplierInvoiceId(p.getSupplierInvoice().getSupplierInvoiceId())
                .grnCode(p.getSupplierInvoice().getGrn().getGrnCode())
                .supplierName(p.getSupplierInvoice().getSupplier().getSupplierName())
                .amountPaid(p.getAmountPaid())
                .paymentMethod(p.getPaymentMethod())
                .paidById(p.getPaidBy().getEmployeeId())
                .paidByName(p.getPaidBy().getFullName())
                .paidAt(p.getPaidAt())
                .build();
    }
}

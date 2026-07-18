package com.retail.service.impl;

import com.retail.dto.SupplierInvoiceResponse;
import com.retail.dto.SupplierInvoiceResponse.SupplierInvoiceResponseBuilder;
import com.retail.entity.*;
import com.retail.exception.ValidationException;
import com.retail.repository.*;
import com.retail.service.SupplierInvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class SupplierInvoiceServiceImpl implements SupplierInvoiceService {

    @Autowired
    private SupplierInvoiceRepository supplierInvoiceRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Override
    public SupplierInvoice createInvoiceFromGrn(GoodsReceiptNote grn) {
        PurchaseOrder po = grn.getPurchaseOrder();
        BigDecimal totalAmount = BigDecimal.ZERO;

        // Map PO details by product ID to quickly lookup contract unit costs
        Map<Long, BigDecimal> poCostMap = po.getDetails().stream().collect(
            Collectors.toMap(d -> d.getProduct().getProductId(), d -> d.getUnitCost(), (c1, c2) -> c1)
        );

        for (GoodsReceiptNoteDetail grnDetail : grn.getDetails()) {
            Long productId = grnDetail.getProduct().getProductId();
            BigDecimal contractUnitCost = poCostMap.getOrDefault(productId, BigDecimal.ZERO);
            BigDecimal qtyReceived = grnDetail.getQuantityReceived();
            BigDecimal lineCost = qtyReceived.multiply(contractUnitCost);
            totalAmount = totalAmount.add(lineCost);
        }

        SupplierInvoice invoice = SupplierInvoice.builder()
                .grn(grn)
                .supplier(po.getSupplier())
                .amount(totalAmount)
                .amountPaid(BigDecimal.ZERO)
                .status("Draft")
                .issuedAt(LocalDateTime.now())
                .build();

        return supplierInvoiceRepository.save(invoice);
    }

    @Override
    public SupplierInvoiceResponse approveInvoice(Long invoiceId, Long employeeId) {
        SupplierInvoice invoice = supplierInvoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ValidationException("Không tìm thấy hóa đơn cần duyệt."));

        if (!"Draft".equals(invoice.getStatus())) {
            throw new ValidationException("Hóa đơn đã được duyệt hoặc xử lý trước đó.");
        }

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ValidationException("Nhân viên duyệt hóa đơn không tồn tại."));

        invoice.setStatus("Unpaid");
        invoice.setApprovedBy(employee);
        invoice.setApprovedAt(LocalDateTime.now());
        SupplierInvoice saved = supplierInvoiceRepository.save(invoice);

        // Write AuditLog
        AuditLog audit = AuditLog.builder()
                .employee(employee)
                .actionType("ApproveSupplierInvoice")
                .entityName("SupplierInvoice")
                .entityId(invoice.getSupplierInvoiceId())
                .oldValue("Draft")
                .newValue("Unpaid")
                .reason("Approved supplier invoice for GRN: " + invoice.getGrn().getGrnCode())
                .build();
        auditLogRepository.save(audit);

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierInvoiceResponse getInvoiceById(Long invoiceId) {
        SupplierInvoice invoice = supplierInvoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ValidationException("Không tìm thấy hóa đơn."));
        return mapToResponse(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SupplierInvoiceResponse> searchInvoices(String keyword, Pageable pageable) {
        return supplierInvoiceRepository.searchInvoices(keyword, pageable).map(this::mapToResponse);
    }

    private SupplierInvoiceResponse mapToResponse(SupplierInvoice s) {
        return SupplierInvoiceResponse.builder()
                .supplierInvoiceId(s.getSupplierInvoiceId())
                .grnId(s.getGrn().getGrnId())
                .grnCode(s.getGrn().getGrnCode())
                .supplierId(s.getSupplier().getSupplierId())
                .supplierName(s.getSupplier().getSupplierName())
                .amount(s.getAmount())
                .amountPaid(s.getAmountPaid())
                .status(s.getStatus())
                .issuedAt(s.getIssuedAt())
                .approvedById(s.getApprovedBy() != null ? s.getApprovedBy().getEmployeeId() : null)
                .approvedByName(s.getApprovedBy() != null ? s.getApprovedBy().getFullName() : null)
                .approvedAt(s.getApprovedAt())
                .build();
    }
}

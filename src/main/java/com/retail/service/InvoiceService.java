package com.retail.service;

import com.retail.dto.*;
import com.retail.dto.InvoiceResponse;
import com.retail.dto.ProductSearchResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface InvoiceService {

    /** 3.1.1 — Khởi tạo hóa đơn mới (Draft), cashierId & branchId từ Security context */
    InvoiceResponse createInvoice();

    /** 3.1.1 — Tìm kiếm sản phẩm theo SKU (exact) hoặc tên (LIKE) */
    Page<ProductSearchResponse> searchProducts(String keyword, String sku, Integer branchId, int page, int size);

    /** 3.1.1 — Thêm sản phẩm vào giỏ (gộp nếu đã có — Rule #10) */
    InvoiceResponse addItem(Long invoiceId, AddInvoiceItemRequest request);

    /** 3.1.1 — Cập nhật số lượng dòng hàng */
    InvoiceResponse updateItem(Long invoiceId, Long detailId, UpdateInvoiceItemRequest request);

    /** 3.1.1 — Xóa dòng hàng khỏi giỏ */
    InvoiceResponse removeItem(Long invoiceId, Long detailId);

    /** 3.1.3 — Thanh toán hóa đơn (atomic transaction) */
    InvoiceResponse payInvoice(Long invoiceId, PayInvoiceRequest request);

    /** 3.1.4 — Draft → Held */
    InvoiceResponse holdInvoice(Long invoiceId);

    /** 3.1.4 — Held → Draft */
    InvoiceResponse resumeInvoice(Long invoiceId);

    /** 3.1.4 — Draft/Held → Canceled */
    InvoiceResponse cancelInvoice(Long invoiceId);

    /** 3.1.5 — Danh sách hóa đơn có filter + phân trang */
    Page<InvoiceResponse> listInvoices(InvoiceSearchRequest request);

    /** 3.1.5 — Chi tiết hóa đơn kèm InvoiceDetail */
    InvoiceResponse getInvoiceDetail(Long invoiceId);

    /** Support for Refund lookup (BUG-05) */
    InvoiceResponse getByCode(String invoiceCode);
}

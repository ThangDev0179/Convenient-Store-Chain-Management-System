package com.retail.procurement;

import com.retail.employee.Employee;
import com.retail.procurement.dto.CreateGoodsReceiptNoteRequest;
import com.retail.procurement.dto.GoodsReceiptNoteResponse;
import org.springframework.data.domain.Page;

public interface GoodsReceiptNoteService {
    GoodsReceiptNoteResponse createGoodsReceiptNote(CreateGoodsReceiptNoteRequest request, Employee user);
    GoodsReceiptNoteResponse getGoodsReceiptNoteById(Long id);
    Page<GoodsReceiptNoteResponse> searchGoodsReceiptNotes(Integer branchId, String keyword, int page, int size);
}

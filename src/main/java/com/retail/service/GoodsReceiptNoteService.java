package com.retail.service;
import com.retail.dto.CreateGoodsReceiptNoteRequest;
import com.retail.entity.Employee;
import com.retail.dto.GoodsReceiptNoteResponse;

import org.springframework.data.domain.Page;

public interface GoodsReceiptNoteService {
    GoodsReceiptNoteResponse createGoodsReceiptNote(CreateGoodsReceiptNoteRequest request, Employee user);
    GoodsReceiptNoteResponse getGoodsReceiptNoteById(Long id);
    Page<GoodsReceiptNoteResponse> searchGoodsReceiptNotes(Integer branchId, String keyword, int page, int size);
}
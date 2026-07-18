package com.retail.service;
import com.retail.dto.ReceiveTransferRequest;
import com.retail.entity.StockTransfer;
import com.retail.dto.StockTransferRequest;


import java.util.List;

public interface StockTransferService {
    StockTransfer createTransfer(StockTransferRequest request, Long createdByEmployeeId);
    StockTransfer approveTransfer(Long transferId, Long approvedByEmployeeId);
    StockTransfer receiveTransfer(Long transferId, ReceiveTransferRequest request, Long receivedByEmployeeId);
    StockTransfer rejectTransfer(Long transferId, Long rejectedByEmployeeId);
    List<StockTransfer> getAllTransfers();
    StockTransfer getTransferById(Long transferId);
}
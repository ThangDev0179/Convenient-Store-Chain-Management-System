package com.retail.transfer.service;

import com.retail.transfer.dto.ReceiveTransferRequest;
import com.retail.transfer.dto.StockTransferRequest;
import com.retail.transfer.entity.StockTransfer;

import java.util.List;

public interface StockTransferService {
    StockTransfer createTransfer(StockTransferRequest request, Long createdByEmployeeId);
    StockTransfer approveTransfer(Long transferId, Long approvedByEmployeeId);
    StockTransfer receiveTransfer(Long transferId, ReceiveTransferRequest request, Long receivedByEmployeeId);
    StockTransfer rejectTransfer(Long transferId, Long rejectedByEmployeeId);
    List<StockTransfer> getAllTransfers();
    StockTransfer getTransferById(Long transferId);
}

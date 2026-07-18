package com.retail.transfer.service.impl;

import com.retail.audit.service.AuditLogService;
import com.retail.branch.Branch;
import com.retail.employee.Employee;
import com.retail.inventory.service.InventoryTransactionService;
import com.retail.procurement.InventoryTransactionType;
import com.retail.procurement.Product;
import com.retail.transfer.dto.ReceiveTransferRequest;
import com.retail.transfer.dto.StockTransferRequest;
import com.retail.transfer.entity.StockTransfer;
import com.retail.transfer.entity.StockTransferDetail;
import com.retail.transfer.entity.StockTransferStatus;
import com.retail.transfer.repository.StockTransferDetailRepository;
import com.retail.transfer.repository.StockTransferRepository;
import com.retail.transfer.service.StockTransferService;
import com.retail.disposal.service.StockDisposalService;
import com.retail.disposal.entity.DisposalSourceType;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockTransferServiceImpl implements StockTransferService {

    private final StockTransferRepository transferRepository;
    private final StockTransferDetailRepository detailRepository;
    private final InventoryTransactionService transactionService;
    private final StockDisposalService disposalService;
    private final AuditLogService auditLogService;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public StockTransfer createTransfer(StockTransferRequest request, Long createdByEmployeeId) {
        if (request.getFromBranchId().equals(request.getToBranchId())) {
            throw new IllegalArgumentException("Chi nhánh gửi và chi nhánh nhận không được giống nhau");
        }

        StockTransfer transfer = new StockTransfer();
        String code = "ST-" + request.getFromBranchId() + "-" + request.getToBranchId()
                + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        transfer.setTransferCode(code);
        transfer.setFromBranch(entityManager.getReference(Branch.class, request.getFromBranchId()));
        transfer.setToBranch(entityManager.getReference(Branch.class, request.getToBranchId()));
        transfer.setStatus(StockTransferStatus.Draft);
        transfer.setCreatedBy(entityManager.getReference(Employee.class, createdByEmployeeId));

        for (StockTransferRequest.TransferDetailDto dto : request.getDetails()) {
            StockTransferDetail detail = new StockTransferDetail();
            detail.setProduct(entityManager.getReference(Product.class, dto.getProductId()));
            detail.setQuantitySent(dto.getQuantitySent());
            transfer.addDetail(detail);
        }

        StockTransfer saved = transferRepository.save(transfer);
        auditLogService.logAction(createdByEmployeeId, "CreateStockTransfer", "StockTransfer",
                saved.getStockTransferId(), null, saved.getStatus().name(), "Tạo phiếu điều chuyển", null, null);
        return saved;
    }

    @Override
    @Transactional
    public StockTransfer approveTransfer(Long transferId, Long approvedByEmployeeId) {
        StockTransfer transfer = getTransferById(transferId);
        if (transfer.getStatus() != StockTransferStatus.Draft) {
            throw new IllegalStateException("Chỉ phiếu Draft mới được phê duyệt");
        }

        String oldStatus = transfer.getStatus().name();
        Integer fromBranchId = transfer.getFromBranch().getBranchId();
        Integer toBranchId = transfer.getToBranch().getBranchId();

        for (StockTransferDetail detail : transfer.getDetails()) {
            Long productId = detail.getProduct().getProductId();
            BigDecimal qty = detail.getQuantitySent();

            // Chi nhánh gửi: Trừ QtyAvailable (cam kết gửi đi, hàng vẫn vật lý ở đây)
            transactionService.recordTransaction(
                    fromBranchId, productId,
                    BigDecimal.ZERO, qty.negate(), BigDecimal.ZERO,
                    InventoryTransactionType.TransferOut,
                    "StockTransfer", transfer.getStockTransferId(),
                    "Duyệt điều chuyển: " + transfer.getTransferCode(),
                    approvedByEmployeeId
            );

            // Chi nhánh nhận: Cộng QtyInTransit (đang vận chuyển đến)
            transactionService.recordTransaction(
                    toBranchId, productId,
                    BigDecimal.ZERO, BigDecimal.ZERO, qty,
                    InventoryTransactionType.TransferIn,
                    "StockTransfer", transfer.getStockTransferId(),
                    "Duyệt điều chuyển: " + transfer.getTransferCode(),
                    approvedByEmployeeId
            );
        }

        transfer.setStatus(StockTransferStatus.In_Transit);
        StockTransfer saved = transferRepository.save(transfer);
        auditLogService.logAction(approvedByEmployeeId, "ApproveStockTransfer", "StockTransfer",
                saved.getStockTransferId(), oldStatus, saved.getStatus().name(), "Phê duyệt điều chuyển", null, null);
        return saved;
    }

    @Override
    @Transactional
    public StockTransfer receiveTransfer(Long transferId, ReceiveTransferRequest request, Long receivedByEmployeeId) {
        StockTransfer transfer = getTransferById(transferId);
        if (transfer.getStatus() != StockTransferStatus.In_Transit) {
            throw new IllegalStateException("Chỉ phiếu In_Transit mới được xác nhận nhận");
        }

        String oldStatus = transfer.getStatus().name();
        Integer fromBranchId = transfer.getFromBranch().getBranchId();
        Integer toBranchId = transfer.getToBranch().getBranchId();

        Map<Long, BigDecimal> receivedMap = request.getDetails().stream()
                .collect(Collectors.toMap(
                        ReceiveTransferRequest.ReceiveDetailDto::getTransferDetailId,
                        ReceiveTransferRequest.ReceiveDetailDto::getQuantityReceived
                ));

        for (StockTransferDetail detail : transfer.getDetails()) {
            BigDecimal qtyReceived = receivedMap.getOrDefault(detail.getTransferDetailId(), BigDecimal.ZERO);
            BigDecimal qtySent = detail.getQuantitySent();
            Long productId = detail.getProduct().getProductId();

            detail.setQuantityReceived(qtyReceived);

            // Chi nhánh gửi: Trừ QtyOnHand (hàng đã rời kho vật lý)
            transactionService.recordTransaction(
                    fromBranchId, productId,
                    qtySent.negate(), BigDecimal.ZERO, BigDecimal.ZERO,
                    InventoryTransactionType.TransferOut,
                    "StockTransfer", transferId,
                    "Xác nhận nhận: " + transfer.getTransferCode(), receivedByEmployeeId
            );

            // Chi nhánh nhận: Trừ QtyInTransit (toàn bộ số lượng đã gửi), cộng QtyOnHand + QtyAvailable (số lượng thực nhận)
            transactionService.recordTransaction(
                    toBranchId, productId,
                    qtyReceived, qtyReceived, qtySent.negate(),
                    InventoryTransactionType.TransferIn,
                    "StockTransfer", transferId,
                    "Xác nhận nhận: " + transfer.getTransferCode(), receivedByEmployeeId
            );

            // Ghi log hao hụt nếu có
            BigDecimal loss = qtySent.subtract(qtyReceived);
            if (loss.compareTo(BigDecimal.ZERO) > 0) {
                auditLogService.logAction(receivedByEmployeeId, "TransferLoss", "StockTransferDetail",
                        detail.getTransferDetailId(), null,
                        "Hao hụt: " + loss + " — sản phẩm ID=" + productId,
                        "Hao hụt trong vận chuyển: " + transfer.getTransferCode(), null, null);
                
                // Tự động sinh phiếu Xuất Hủy (Disposal) để ghi nhận chi phí/báo cáo
                disposalService.autoCreateFromLoss(toBranchId, productId, loss, 
                        DisposalSourceType.TransferLoss, transferId, 
                        "Hao hụt vận chuyển phiếu: " + transfer.getTransferCode(), receivedByEmployeeId);
            }
        }

        transfer.setStatus(StockTransferStatus.Completed);
        transfer.setReceivedBy(entityManager.getReference(Employee.class, receivedByEmployeeId));
        transfer.setReceivedAt(LocalDateTime.now());
        StockTransfer saved = transferRepository.save(transfer);
        auditLogService.logAction(receivedByEmployeeId, "ReceiveStockTransfer", "StockTransfer",
                saved.getStockTransferId(), oldStatus, saved.getStatus().name(), "Xác nhận nhận hàng", null, null);
        return saved;
    }

    @Override
    @Transactional
    public StockTransfer rejectTransfer(Long transferId, Long rejectedByEmployeeId) {
        StockTransfer transfer = getTransferById(transferId);
        if (transfer.getStatus() != StockTransferStatus.Draft) {
            throw new IllegalStateException("Chỉ phiếu Draft mới được từ chối");
        }
        String oldStatus = transfer.getStatus().name();
        transfer.setStatus(StockTransferStatus.Rejected);
        StockTransfer saved = transferRepository.save(transfer);
        auditLogService.logAction(rejectedByEmployeeId, "RejectStockTransfer", "StockTransfer",
                saved.getStockTransferId(), oldStatus, saved.getStatus().name(), "Từ chối điều chuyển", null, null);
        return saved;
    }

    @Override
    public List<StockTransfer> getAllTransfers() {
        return transferRepository.findAll();
    }

    @Override
    public StockTransfer getTransferById(Long transferId) {
        return transferRepository.findById(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu điều chuyển ID: " + transferId));
    }
}

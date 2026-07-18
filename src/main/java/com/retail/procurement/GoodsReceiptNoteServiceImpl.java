package com.retail.procurement;

import com.retail.branch.Branch;
import com.retail.branch.BranchRepository;
import com.retail.employee.Employee;
import com.retail.exception.ValidationException;
import com.retail.audit.AuditLog;
import com.retail.audit.AuditLogRepository;
import com.retail.procurement.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class GoodsReceiptNoteServiceImpl implements GoodsReceiptNoteService {

    @Autowired
    private GoodsReceiptNoteRepository grnRepository;

    @Autowired
    private GoodsReceiptNoteDetailRepository grnDetailRepository;

    @Autowired
    private PurchaseOrderRepository poRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductUOMRepository productUOMRepository;

    @Autowired
    private BranchInventoryRepository branchInventoryRepository;

    @Autowired
    private InventoryTransactionHistoryRepository inventoryTransactionHistoryRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Override
    public GoodsReceiptNoteResponse createGoodsReceiptNote(CreateGoodsReceiptNoteRequest request, Employee user) {
        PurchaseOrder po = poRepository.findById(request.getPurchaseOrderId())
                .orElseThrow(() -> new ValidationException("Không tìm thấy đơn đặt hàng PO liên kết."));

        if (po.getStatus() != PurchaseOrderStatus.Submitted && po.getStatus() != PurchaseOrderStatus.Partially_Received) {
            throw new ValidationException("Đơn đặt hàng PO phải ở trạng thái Đã gửi duyệt hoặc Đang nhập kho dở dang mới có thể nhập kho.");
        }

        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new ValidationException("Chi nhánh không tồn tại."));

        // Generate GrnCode: GRN-[Mã Chi Nhánh]-YYYYMMDD-[4 số tăng tự động]
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "GRN-" + branch.getBranchCode() + "-" + dateStr + "-";
        String maxCode = grnRepository.findMaxGrnCodeByBranchAndDate(branch.getBranchCode(), dateStr);
        int nextSeq = 1;
        if (maxCode != null) {
            try {
                String seqStr = maxCode.substring(maxCode.lastIndexOf("-") + 1);
                nextSeq = Integer.parseInt(seqStr) + 1;
            } catch (Exception e) {
                // Ignore parsing errors
            }
        }
        String grnCode = prefix + String.format("%04d", nextSeq);

        GoodsReceiptNote grn = GoodsReceiptNote.builder()
                .grnCode(grnCode)
                .purchaseOrder(po)
                .branch(branch)
                .receivedDate(LocalDateTime.now())
                .receivedBy(user)
                .status("Completed")
                .totalCost(BigDecimal.ZERO)
                .build();

        // Save first to obtain GRN ID
        GoodsReceiptNote savedGrn = grnRepository.save(grn);

        BigDecimal totalCost = BigDecimal.ZERO;
        List<GoodsReceiptNoteDetail> details = new ArrayList<>();

        for (GoodsReceiptNoteDetailRequest detailReq : request.getDetails()) {
            Product product = productRepository.findById(detailReq.getProductId())
                    .orElseThrow(() -> new ValidationException("Sản phẩm không tồn tại."));

            if (product.getStatus() == ProductStatus.Inactive) {
                throw new ValidationException("Sản phẩm " + product.getProductName() + " đã ngừng kinh doanh.");
            }

            ProductUOM uom = productUOMRepository.findById(detailReq.getUomId())
                    .orElseThrow(() -> new ValidationException("Đơn vị tính không tồn tại."));

            if (!uom.getProduct().getProductId().equals(product.getProductId())) {
                throw new ValidationException("Đơn vị tính không khớp với sản phẩm " + product.getProductName());
            }

            if (detailReq.getQuantityReceived().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException("Số lượng thực nhận phải lớn hơn 0.");
            }

            if (detailReq.getUnitCost().compareTo(BigDecimal.ZERO) < 0) {
                throw new ValidationException("Đơn giá nhập kho không được âm.");
            }

            BigDecimal rowCost = detailReq.getQuantityReceived().multiply(detailReq.getUnitCost());
            totalCost = totalCost.add(rowCost);

            GoodsReceiptNoteDetail detail = GoodsReceiptNoteDetail.builder()
                    .goodsReceiptNote(savedGrn)
                    .product(product)
                    .uom(uom)
                    .quantityOrdered(detailReq.getQuantityOrdered())
                    .quantityReceived(detailReq.getQuantityReceived())
                    .unitCost(detailReq.getUnitCost())
                    .totalCost(rowCost)
                    .build();

            grnDetailRepository.save(detail);
            details.add(detail);

            // Update Branch Inventory in Base Unit Quantity
            BigDecimal baseQtyReceived = detailReq.getQuantityReceived().multiply(uom.getConversionRate());
            
            BranchInventory inventory = branchInventoryRepository.findByBranchBranchIdAndProductProductId(branch.getBranchId(), product.getProductId())
                    .orElseGet(() -> BranchInventory.builder()
                            .branch(branch)
                            .product(product)
                            .qtyOnHand(BigDecimal.ZERO)
                            .qtyAvailable(BigDecimal.ZERO)
                            .qtyInTransit(BigDecimal.ZERO)
                            .build());

            BigDecimal qtyAfter = inventory.getQtyOnHand().add(baseQtyReceived);
            inventory.setQtyOnHand(qtyAfter);
            inventory.setQtyAvailable(inventory.getQtyAvailable().add(baseQtyReceived));
            inventory.setUpdatedAt(LocalDateTime.now());
            branchInventoryRepository.save(inventory);

            // Log Inventory Transaction History
            InventoryTransactionHistory history = InventoryTransactionHistory.builder()
                    .branch(branch)
                    .product(product)
                    .transactionType(InventoryTransactionType.GRN)
                    .referenceTable("GoodsReceiptNote")
                    .referenceId(savedGrn.getGrnId())
                    .quantityChange(baseQtyReceived)
                    .createdBy(user)
                    .build();
            inventoryTransactionHistoryRepository.save(history);
        }

        savedGrn.setDetails(details);
        savedGrn.setTotalCost(totalCost);
        grnRepository.save(savedGrn);

        // Update status of the Purchase Order
        updatePurchaseOrderStatus(po);

        // Write AuditLog
        AuditLog audit = AuditLog.builder()
                .employee(user)
                .actionType("CreateGoodsReceiptNote")
                .entityName("GoodsReceiptNote")
                .entityId(savedGrn.getGrnId())
                .oldValue(null)
                .newValue("{\"grnCode\":\"" + grnCode + "\",\"totalCost\":" + totalCost + "}")
                .reason("Imported products from PO: " + po.getPoCode())
                .build();
        auditLogRepository.save(audit);

        return mapToResponse(savedGrn);
    }

    @Override
    public GoodsReceiptNoteResponse getGoodsReceiptNoteById(Long id) {
        GoodsReceiptNote grn = grnRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Không tìm thấy phiếu nhập kho."));
        return mapToResponse(grn);
    }

    @Override
    public Page<GoodsReceiptNoteResponse> searchGoodsReceiptNotes(Integer branchId, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        // Simple search query using JpaRepository findAll, map to DTO
        // If branchId is not null, filter by branchId.
        Page<GoodsReceiptNote> grnPage;
        if (branchId != null) {
            // Find all and filter in memory for simplicity or implement repository filter.
            // Let's implement in memory filter or fetch accordingly.
            List<GoodsReceiptNote> all = grnRepository.findAll(Sort.by("createdAt").descending());
            List<GoodsReceiptNote> filtered = all.stream()
                    .filter(g -> g.getBranch().getBranchId().equals(branchId))
                    .filter(g -> keyword == null || keyword.trim().isEmpty() || g.getGrnCode().contains(keyword) || g.getPurchaseOrder().getPoCode().contains(keyword))
                    .collect(Collectors.toList());
            
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), filtered.size());
            List<GoodsReceiptNote> subList = start > filtered.size() ? new ArrayList<>() : filtered.subList(start, end);
            grnPage = new PageImpl<>(subList, pageable, filtered.size());
        } else {
            List<GoodsReceiptNote> all = grnRepository.findAll(Sort.by("createdAt").descending());
            List<GoodsReceiptNote> filtered = all.stream()
                    .filter(g -> keyword == null || keyword.trim().isEmpty() || g.getGrnCode().contains(keyword) || g.getPurchaseOrder().getPoCode().contains(keyword))
                    .collect(Collectors.toList());
            
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), filtered.size());
            List<GoodsReceiptNote> subList = start > filtered.size() ? new ArrayList<>() : filtered.subList(start, end);
            grnPage = new PageImpl<>(subList, pageable, filtered.size());
        }
        return grnPage.map(this::mapToResponse);
    }

    private void updatePurchaseOrderStatus(PurchaseOrder po) {
        // Find all GRNs associated with this PO that are "Completed"
        List<GoodsReceiptNote> grns = grnRepository.findByPurchaseOrderPurchaseOrderIdAndStatus(po.getPurchaseOrderId(), "Completed");

        // Map of ProductId -> Total Quantity Received so far in base units
        Map<Long, BigDecimal> receivedMap = new HashMap<>();
        for (GoodsReceiptNote grn : grns) {
            for (GoodsReceiptNoteDetail detail : grn.getDetails()) {
                Long productId = detail.getProduct().getProductId();
                BigDecimal conversionRate = detail.getUom().getConversionRate();
                BigDecimal baseQty = detail.getQuantityReceived().multiply(conversionRate);

                receivedMap.put(productId, receivedMap.getOrDefault(productId, BigDecimal.ZERO).add(baseQty));
            }
        }

        boolean allCompleted = true;
        boolean atLeastOneReceived = false;

        for (PurchaseOrderDetail poDetail : po.getDetails()) {
            Long productId = poDetail.getProduct().getProductId();
            BigDecimal baseQtyOrdered = poDetail.getQuantityOrdered().multiply(poDetail.getUom().getConversionRate());

            BigDecimal baseQtyReceived = receivedMap.getOrDefault(productId, BigDecimal.ZERO);

            if (baseQtyReceived.compareTo(BigDecimal.ZERO) > 0) {
                atLeastOneReceived = true;
            }

            if (baseQtyReceived.compareTo(baseQtyOrdered) < 0) {
                allCompleted = false;
            }
        }

        if (allCompleted) {
            po.setStatus(PurchaseOrderStatus.Received_Partial); // Received_Partial is used as Completed status
        } else if (atLeastOneReceived) {
            po.setStatus(PurchaseOrderStatus.Partially_Received);
        } else {
            po.setStatus(PurchaseOrderStatus.Submitted);
        }
        poRepository.save(po);
    }

    private GoodsReceiptNoteResponse mapToResponse(GoodsReceiptNote grn) {
        List<GoodsReceiptNoteDetailResponse> details = grn.getDetails().stream().map(d -> 
            GoodsReceiptNoteDetailResponse.builder()
                    .grnDetailId(d.getGrnDetailId())
                    .productId(d.getProduct().getProductId())
                    .productSku(d.getProduct().getSku())
                    .productName(d.getProduct().getProductName())
                    .uomId(d.getUom().getUomId())
                    .uomName(d.getUom().getUomName())
                    .quantityOrdered(d.getQuantityOrdered())
                    .quantityReceived(d.getQuantityReceived())
                    .unitCost(d.getUnitCost())
                    .totalCost(d.getTotalCost())
                    .build()
        ).collect(Collectors.toList());

        return GoodsReceiptNoteResponse.builder()
                .grnId(grn.getGrnId())
                .grnCode(grn.getGrnCode())
                .purchaseOrderId(grn.getPurchaseOrder().getPurchaseOrderId())
                .purchaseOrderCode(grn.getPurchaseOrder().getPoCode())
                .branchId(grn.getBranch().getBranchId())
                .branchName(grn.getBranch().getBranchName())
                .receivedDate(grn.getReceivedDate())
                .receivedById(grn.getReceivedBy().getEmployeeId())
                .receivedByName(grn.getReceivedBy().getFullName())
                .status(grn.getStatus())
                .totalCost(grn.getTotalCost())
                .createdAt(grn.getCreatedAt())
                .details(details)
                .build();
    }
}

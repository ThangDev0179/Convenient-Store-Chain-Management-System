package com.retail.service.impl;
import com.retail.entity.AuditLog;
import com.retail.repository.AuditLogRepository;
import com.retail.entity.Branch;
import com.retail.entity.BranchInventory;
import com.retail.repository.BranchInventoryRepository;
import com.retail.repository.BranchRepository;
import com.retail.dto.CreateGoodsReceiptNoteRequest;
import com.retail.entity.Employee;
import com.retail.entity.GoodsReceiptNote;
import com.retail.entity.GoodsReceiptNoteDetail;
import com.retail.repository.GoodsReceiptNoteDetailRepository;
import com.retail.dto.GoodsReceiptNoteDetailRequest;
import com.retail.dto.GoodsReceiptNoteDetailResponse;
import com.retail.repository.GoodsReceiptNoteRepository;
import com.retail.dto.GoodsReceiptNoteResponse;
import com.retail.service.GoodsReceiptNoteService;
import com.retail.entity.InventoryTransactionHistory;
import com.retail.repository.InventoryTransactionHistoryRepository;
import com.retail.entity.InventoryTransactionType;
import com.retail.entity.Product;
import com.retail.repository.ProductRepository;
import com.retail.entity.ProductStatus;
import com.retail.entity.ProductUOM;
import com.retail.repository.ProductUOMRepository;
import com.retail.entity.PurchaseOrder;
import com.retail.entity.PurchaseOrderDetail;
import com.retail.repository.PurchaseOrderRepository;
import com.retail.entity.PurchaseOrderStatus;
import com.retail.exception.ValidationException;

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

    @Autowired
    private com.retail.service.SupplierInvoiceService supplierInvoiceService;

    @Override
    public GoodsReceiptNoteResponse createGoodsReceiptNote(CreateGoodsReceiptNoteRequest request, Employee user) {
        PurchaseOrder po = poRepository.findById(request.getPurchaseOrderId())
                .orElseThrow(() -> new ValidationException("Không tìm thấy đơn đặt hàng PO liên kết."));

        if (po.getStatus() != PurchaseOrderStatus.Approved && po.getStatus() != PurchaseOrderStatus.Partially_Received) {
            throw new ValidationException("Đơn đặt hàng PO phải ở trạng thái Đã phê duyệt hoặc Đang nhập kho dở dang mới có thể nhập kho.");
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
                .receivedAt(LocalDateTime.now())
                .receivedBy(user)
                .status("Completed")
                .build();

        // Save first to obtain GRN ID
        GoodsReceiptNote savedGrn = grnRepository.save(grn);

        BigDecimal totalCost = BigDecimal.ZERO;
        List<GoodsReceiptNoteDetail> details = new ArrayList<>();
        java.util.Set<Long> productIds = new java.util.HashSet<>();

        for (GoodsReceiptNoteDetailRequest detailReq : request.getDetails()) {
            if (!productIds.add(detailReq.getProductId())) {
                throw new ValidationException("Sản phẩm ID " + detailReq.getProductId() + " bị trùng lặp trong danh sách nhận hàng.");
            }
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

            // Update Branch Inventory in Base Unit Quantity
            BigDecimal baseQtyReceived = detailReq.getQuantityReceived().multiply(uom.getConversionRate());

            GoodsReceiptNoteDetail detail = GoodsReceiptNoteDetail.builder()
                    .goodsReceiptNote(savedGrn)
                    .product(product)
                    .uom(uom)
                    .quantityReceived(detailReq.getQuantityReceived())
                    .quantityConvertedBase(baseQtyReceived)
                    .build();

            grnDetailRepository.save(detail);
            details.add(detail);

            // Already calculated above
            // Update Branch Inventory in Base Unit Quantity
            
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
        grnRepository.save(savedGrn);

        // Update status of the Purchase Order
        updatePurchaseOrderStatus(po);

        // Auto create Supplier Invoice (Draft) for the GRN
        supplierInvoiceService.createInvoiceFromGrn(savedGrn);

        // Write AuditLog
        AuditLog audit = AuditLog.builder()
                .employee(user)
                .actionType("CreateGoodsReceiptNote")
                .entityName("GoodsReceiptNote")
                .entityId(savedGrn.getGrnId())
                .oldValue(null)
                .newValue("{\"grnCode\":\"" + grnCode + "\"}")
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
            po.setStatus(PurchaseOrderStatus.Completed);
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
                    .quantityReceived(d.getQuantityReceived())
                    .quantityConvertedBase(d.getQuantityConvertedBase())
                    .build()
        ).collect(Collectors.toList());

        return GoodsReceiptNoteResponse.builder()
                .grnId(grn.getGrnId())
                .grnCode(grn.getGrnCode())
                .purchaseOrderId(grn.getPurchaseOrder().getPurchaseOrderId())
                .purchaseOrderCode(grn.getPurchaseOrder().getPoCode())
                .branchId(grn.getBranch().getBranchId())
                .branchName(grn.getBranch().getBranchName())
                .receivedAt(grn.getReceivedAt())
                .receivedById(grn.getReceivedBy().getEmployeeId())
                .receivedByName(grn.getReceivedBy().getFullName())
                .status(grn.getStatus())
                .createdAt(grn.getCreatedAt())
                .details(details)
                .build();
    }
}
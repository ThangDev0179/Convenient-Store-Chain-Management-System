package com.retail.service.impl;
import com.retail.entity.AuditLog;
import com.retail.repository.AuditLogRepository;
import com.retail.entity.Branch;
import com.retail.repository.BranchRepository;
import com.retail.dto.CreatePurchaseOrderRequest;
import com.retail.entity.Employee;
import com.retail.entity.Product;
import com.retail.repository.ProductRepository;
import com.retail.entity.ProductStatus;
import com.retail.entity.ProductUOM;
import com.retail.repository.ProductUOMRepository;
import com.retail.entity.PurchaseOrder;
import com.retail.entity.PurchaseOrderDetail;
import com.retail.repository.PurchaseOrderDetailRepository;
import com.retail.dto.PurchaseOrderDetailRequest;
import com.retail.dto.PurchaseOrderDetailResponse;
import com.retail.repository.PurchaseOrderRepository;
import com.retail.dto.PurchaseOrderResponse;
import com.retail.service.PurchaseOrderService;
import com.retail.entity.PurchaseOrderStatus;
import com.retail.entity.Supplier;
import com.retail.repository.SupplierRepository;
import com.retail.entity.SupplierStatus;
import com.retail.exception.ValidationException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    @Autowired
    private PurchaseOrderRepository poRepository;

    @Autowired
    private PurchaseOrderDetailRepository poDetailRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductUOMRepository productUOMRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Override
    public PurchaseOrderResponse createPurchaseOrder(CreatePurchaseOrderRequest request, Employee creator) {
        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new ValidationException("Nhà cung cấp không tồn tại"));

        if (supplier.getStatus() == SupplierStatus.Inactive) {
            throw new ValidationException("Nhà cung cấp đã ngừng hoạt động, không thể đặt hàng.");
        }

        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new ValidationException("Chi nhánh không tồn tại"));

        // Generate Code: PO-[Mã Chi Nhánh]-YYYYMMDD-[4 số tăng tự động]
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "PO-" + branch.getBranchCode() + "-" + dateStr + "-";
        
        String maxCode = poRepository.findMaxPoCodeByBranchAndDate(branch.getBranchCode(), dateStr);
        int nextSeq = 1;
        if (maxCode != null) {
            try {
                String seqStr = maxCode.substring(maxCode.lastIndexOf("-") + 1);
                nextSeq = Integer.parseInt(seqStr) + 1;
            } catch (Exception e) {
                // fallback
            }
        }
        String poCode = prefix + String.format("%04d", nextSeq);

        PurchaseOrder po = PurchaseOrder.builder()
                .poCode(poCode)
                .branch(branch)
                .supplier(supplier)
                .status(PurchaseOrderStatus.Draft)
                .createdBy(creator)
                .build();

        List<PurchaseOrderDetail> details = new ArrayList<>();
        java.util.Set<Long> productIds = new java.util.HashSet<>();
        for (PurchaseOrderDetailRequest detailReq : request.getDetails()) {
            if (!productIds.add(detailReq.getProductId())) {
                throw new ValidationException("Sản phẩm ID " + detailReq.getProductId() + " bị trùng lặp trong danh sách đặt hàng.");
            }
            Product product = productRepository.findById(detailReq.getProductId())
                    .orElseThrow(() -> new ValidationException("Sản phẩm không tồn tại"));

            if (product.getStatus() == ProductStatus.Inactive) {
                throw new ValidationException("Sản phẩm " + product.getProductName() + " đã ngừng kinh doanh.");
            }

            ProductUOM uom = productUOMRepository.findById(detailReq.getUomId())
                    .orElseThrow(() -> new ValidationException("Đơn vị tính không tồn tại"));

            if (!uom.getProduct().getProductId().equals(product.getProductId())) {
                throw new ValidationException("Đơn vị tính không khớp với sản phẩm " + product.getProductName());
            }

            if (detailReq.getQuantityOrdered().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException("Số lượng đặt của sản phẩm " + product.getProductName() + " phải lớn hơn 0.");
            }

            if (detailReq.getUnitCost().compareTo(BigDecimal.ZERO) < 0) {
                throw new ValidationException("Giá nhập của sản phẩm " + product.getProductName() + " không được âm.");
            }

            PurchaseOrderDetail detail = PurchaseOrderDetail.builder()
                    .purchaseOrder(po)
                    .product(product)
                    .uom(uom)
                    .quantityOrdered(detailReq.getQuantityOrdered())
                    .unitCost(detailReq.getUnitCost())
                    .build();

            details.add(detail);
        }

        po.setDetails(details);
        PurchaseOrder savedPo = poRepository.save(po);
        return mapToResponse(savedPo);
    }

    @Override
    public PurchaseOrderResponse submitPurchaseOrder(Long poId, Employee user) {
        PurchaseOrder po = poRepository.findById(poId)
                .orElseThrow(() -> new ValidationException("Không tìm thấy đơn đặt hàng"));

        if (po.getStatus() != PurchaseOrderStatus.Draft) {
            throw new ValidationException("Chỉ đơn đặt hàng ở trạng thái Nháp mới có thể gửi duyệt");
        }

        po.setStatus(PurchaseOrderStatus.Submitted);
        return mapToResponse(poRepository.save(po));
    }

    @Override
    public PurchaseOrderResponse cancelPurchaseOrder(Long poId, Employee user) {
        PurchaseOrder po = poRepository.findById(poId)
                .orElseThrow(() -> new ValidationException("Không tìm thấy đơn đặt hàng"));

        if (po.getStatus() != PurchaseOrderStatus.Draft && po.getStatus() != PurchaseOrderStatus.Submitted) {
            throw new ValidationException("Chỉ đơn đặt hàng ở trạng thái Nháp hoặc Chờ duyệt mới có thể hủy");
        }

        String oldStatus = po.getStatus().name();
        po.setStatus(PurchaseOrderStatus.Canceled);
        PurchaseOrder saved = poRepository.save(po);

        // Write AuditLog
        AuditLog audit = AuditLog.builder()
                .employee(user)
                .actionType("CancelPurchaseOrder")
                .entityName("PurchaseOrder")
                .entityId(po.getPurchaseOrderId())
                .oldValue("{\"status\":\"" + oldStatus + "\"}")
                .newValue("{\"status\":\"Canceled\"}")
                .reason("Manager/Admin requested cancellation")
                .build();
        auditLogRepository.save(audit);

        return mapToResponse(saved);
    }

    @Override
    public PurchaseOrderResponse approvePurchaseOrder(Long poId, Employee user) {
        PurchaseOrder po = poRepository.findById(poId)
                .orElseThrow(() -> new ValidationException("Không tìm thấy đơn đặt hàng"));

        if (po.getStatus() != PurchaseOrderStatus.Submitted) {
            throw new ValidationException("Chỉ đơn đặt hàng ở trạng thái Chờ duyệt mới có thể phê duyệt");
        }

        po.setStatus(PurchaseOrderStatus.Approved);
        PurchaseOrder saved = poRepository.save(po);

        AuditLog audit = AuditLog.builder()
                .employee(user)
                .actionType("ApprovePurchaseOrder")
                .entityName("PurchaseOrder")
                .entityId(po.getPurchaseOrderId())
                .oldValue("{\"status\":\"Submitted\"}")
                .newValue("{\"status\":\"Approved\"}")
                .reason("Admin approved PO")
                .build();
        auditLogRepository.save(audit);

        return mapToResponse(saved);
    }

    @Override
    public PurchaseOrderResponse rejectPurchaseOrder(Long poId, Employee user) {
        PurchaseOrder po = poRepository.findById(poId)
                .orElseThrow(() -> new ValidationException("Không tìm thấy đơn đặt hàng"));

        if (po.getStatus() != PurchaseOrderStatus.Submitted) {
            throw new ValidationException("Chỉ đơn đặt hàng ở trạng thái Chờ duyệt mới có thể từ chối");
        }

        po.setStatus(PurchaseOrderStatus.Rejected);
        PurchaseOrder saved = poRepository.save(po);

        AuditLog audit = AuditLog.builder()
                .employee(user)
                .actionType("RejectPurchaseOrder")
                .entityName("PurchaseOrder")
                .entityId(po.getPurchaseOrderId())
                .oldValue("{\"status\":\"Submitted\"}")
                .newValue("{\"status\":\"Rejected\"}")
                .reason("Admin rejected PO")
                .build();
        auditLogRepository.save(audit);

        return mapToResponse(saved);
    }

    @Override
    public PurchaseOrderResponse getPurchaseOrderById(Long poId) {
        PurchaseOrder po = poRepository.findById(poId)
                .orElseThrow(() -> new ValidationException("Không tìm thấy đơn đặt hàng"));
        return mapToResponse(po);
    }

    @Override
    public Page<PurchaseOrderResponse> searchPurchaseOrders(Integer branchId, String statusStr, String keyword, int page, int size) {
        PurchaseOrderStatus status = null;
        if (statusStr != null && !statusStr.trim().isEmpty()) {
            try {
                status = PurchaseOrderStatus.valueOf(statusStr);
            } catch (IllegalArgumentException e) {
                // ignore
            }
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PurchaseOrder> poPage = poRepository.searchPurchaseOrders(branchId, status, keyword, pageable);
        return poPage.map(this::mapToResponse);
    }

    private PurchaseOrderResponse mapToResponse(PurchaseOrder po) {
        BigDecimal totalCost = BigDecimal.ZERO;
        List<PurchaseOrderDetailResponse> detailResponses = new ArrayList<>();

        if (po.getDetails() != null) {
            for (PurchaseOrderDetail detail : po.getDetails()) {
                BigDecimal lineTotal = BigDecimal.ZERO;
                if (detail.getQuantityOrdered() != null && detail.getUnitCost() != null) {
                    lineTotal = detail.getQuantityOrdered().multiply(detail.getUnitCost());
                    totalCost = totalCost.add(lineTotal);
                }

                detailResponses.add(PurchaseOrderDetailResponse.builder()
                        .poDetailId(detail.getPoDetailId())
                        .productId(detail.getProduct().getProductId())
                        .sku(detail.getProduct().getSku())
                        .productName(detail.getProduct().getProductName())
                        .uomId(detail.getUom().getUomId())
                        .uomName(detail.getUom().getUomName())
                        .quantityOrdered(detail.getQuantityOrdered())
                        .unitCost(detail.getUnitCost())
                        .lineTotal(lineTotal)
                        .build());
            }
        }

        return PurchaseOrderResponse.builder()
                .purchaseOrderId(po.getPurchaseOrderId())
                .poCode(po.getPoCode())
                .branchId(po.getBranch().getBranchId())
                .branchName(po.getBranch().getBranchName())
                .supplierId(po.getSupplier().getSupplierId())
                .supplierName(po.getSupplier().getSupplierName())
                .status(po.getStatus().name())
                .createdById(po.getCreatedBy().getEmployeeId())
                .createdByName(po.getCreatedBy().getFullName())
                .createdAt(po.getCreatedAt())
                .details(detailResponses)
                .totalCost(totalCost)
                .build();
    }
}
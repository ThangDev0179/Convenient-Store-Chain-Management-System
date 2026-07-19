package com.retail.service.impl;

import com.retail.entity.Branch;
import com.retail.entity.BranchPriceRequest;
import com.retail.entity.BranchProductPrice;
import com.retail.entity.BranchProductPriceId;
import com.retail.entity.Employee;
import com.retail.entity.Product;
import com.retail.exception.ValidationException;
import com.retail.repository.BranchPriceRequestRepository;
import com.retail.repository.BranchProductPriceRepository;
import com.retail.service.BranchPriceService;
import com.retail.service.AuditLogService;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BranchPriceServiceImpl implements BranchPriceService {

    private final BranchPriceRequestRepository requestRepository;
    private final BranchProductPriceRepository productPriceRepository;
    private final AuditLogService auditLogService;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public BranchPriceRequest createRequest(Integer branchId, Long productId, BigDecimal proposedPrice, Long employeeId) {
        Product product = entityManager.find(Product.class, productId);
        if (product == null) {
            throw new ValidationException("Sản phẩm không tồn tại");
        }

        BigDecimal stdPrice = product.getStandardPrice() != null ? product.getStandardPrice() : BigDecimal.ZERO;

        BranchPriceRequest request = BranchPriceRequest.builder()
                .branch(entityManager.getReference(Branch.class, branchId))
                .product(product)
                .proposedPrice(proposedPrice)
                .standardPriceSnapshot(stdPrice)
                .status("Pending")
                .requestedBy(entityManager.getReference(Employee.class, employeeId))
                .build();

        BranchPriceRequest saved = requestRepository.save(request);
        auditLogService.logAction(employeeId, "CreatePriceRequest", "BranchPriceRequest",
                saved.getPriceRequestId(), null, "Pending", "Đề xuất giá riêng chi nhánh", null, null);
        return saved;
    }

    @Override
    @Transactional
    public BranchPriceRequest approveRequest(Long requestId, Long employeeId) {
        BranchPriceRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ValidationException("Yêu cầu thay đổi giá không tồn tại"));

        if (!"Pending".equals(request.getStatus())) {
            throw new ValidationException("Chỉ yêu cầu đang chờ duyệt mới được phê duyệt");
        }

        // Validate chênh lệch không vượt quá 20% so với giá chuẩn gốc tại thời điểm duyệt
        BigDecimal stdPrice = request.getProduct().getStandardPrice() != null ? request.getProduct().getStandardPrice() : BigDecimal.ZERO;
        BigDecimal proposed = request.getProposedPrice();
        BigDecimal diff = proposed.subtract(stdPrice).abs();
        BigDecimal maxDiff = stdPrice.multiply(new BigDecimal("0.20"));

        if (diff.compareTo(maxDiff) > 0) {
            throw new ValidationException("Giá đề xuất " + proposed + " vượt quá giới hạn chênh lệch 20% so với giá tiêu chuẩn hiện tại: " + stdPrice);
        }

        request.setStatus("Approved");
        request.setApprovedBy(entityManager.getReference(Employee.class, employeeId));
        request.setApprovedAt(LocalDateTime.now());
        BranchPriceRequest saved = requestRepository.save(request);

        // Cập nhật giá bán tại chi nhánh
        BranchProductPrice price = productPriceRepository.findByBranchBranchIdAndProductProductId(
                request.getBranch().getBranchId(), request.getProduct().getProductId()).orElse(null);

        if (price == null) {
            price = BranchProductPrice.builder()
                    .branch(request.getBranch())
                    .product(request.getProduct())
                    .effectivePrice(proposed)
                    .sourcePriceRequest(saved)
                    .effectiveFrom(LocalDateTime.now())
                    .build();
        } else {
            price.setEffectivePrice(proposed);
            price.setSourcePriceRequest(saved);
            price.setEffectiveFrom(LocalDateTime.now());
        }
        productPriceRepository.save(price);

        auditLogService.logAction(employeeId, "ApprovePriceRequest", "BranchPriceRequest",
                saved.getPriceRequestId(), "Pending", "Approved", "Phê duyệt giá riêng chi nhánh", null, null);
        return saved;
    }

    @Override
    @Transactional
    public BranchPriceRequest rejectRequest(Long requestId, Long employeeId) {
        BranchPriceRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ValidationException("Yêu cầu thay đổi giá không tồn tại"));

        if (!"Pending".equals(request.getStatus())) {
            throw new ValidationException("Chỉ yêu cầu đang chờ duyệt mới được từ chối");
        }

        request.setStatus("Rejected");
        request.setApprovedBy(entityManager.getReference(Employee.class, employeeId));
        request.setApprovedAt(LocalDateTime.now());
        BranchPriceRequest saved = requestRepository.save(request);

        auditLogService.logAction(employeeId, "RejectPriceRequest", "BranchPriceRequest",
                saved.getPriceRequestId(), "Pending", "Rejected", "Từ chối đề xuất giá", null, null);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getEffectivePrice(Integer branchId, Long productId) {
        BranchProductPrice price = productPriceRepository.findByBranchBranchIdAndProductProductId(branchId, productId).orElse(null);
        if (price != null) {
            return price.getEffectivePrice();
        }
        Product product = entityManager.find(Product.class, productId);
        return (product != null && product.getStandardPrice() != null) ? product.getStandardPrice() : BigDecimal.ZERO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BranchPriceRequest> getRequestsByBranch(Integer branchId) {
        return requestRepository.findByBranchBranchId(branchId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BranchPriceRequest> getAllRequests() {
        return requestRepository.findAll();
    }
}

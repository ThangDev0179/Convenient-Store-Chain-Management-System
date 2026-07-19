package com.retail.service.impl;

import com.retail.dto.PromotionDetailRequest;
import com.retail.dto.PromotionDetailResponse;
import com.retail.dto.PromotionRequest;
import com.retail.dto.PromotionResponse;
import com.retail.entity.*;
import com.retail.exception.ValidationException;
import com.retail.repository.AuditLogRepository;
import com.retail.repository.EmployeeRepository;
import com.retail.repository.ProductRepository;
import com.retail.repository.PromotionDetailRepository;
import com.retail.repository.PromotionRepository;
import com.retail.service.PromotionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class PromotionServiceImpl implements PromotionService {

    @Autowired
    private PromotionRepository promotionRepository;

    @Autowired
    private PromotionDetailRepository promotionDetailRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired(required = false)
    private jakarta.servlet.http.HttpServletRequest currentRequest;

    private void logAudit(String actionType, String entityName, Long entityId, String oldValue, String newValue, String reason) {
        Employee employee = null;
        try {
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                employee = employeeRepository.findByUsername(auth.getName()).orElse(null);
            }
        } catch (Exception ignored) {}

        String ip = null;
        String userAgent = null;
        if (currentRequest != null) {
            ip = currentRequest.getRemoteAddr();
            userAgent = currentRequest.getHeader("User-Agent");
        }

        AuditLog log = AuditLog.builder()
                .employee(employee)
                .actionType(actionType)
                .entityName(entityName)
                .entityId(entityId)
                .oldValue(oldValue)
                .newValue(newValue)
                .reason(reason)
                .ipAddress(ip)
                .deviceId(userAgent)
                .build();
        auditLogRepository.save(log);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PromotionResponse> list(String keyword, PromotionStatus status, Pageable pageable) {
        String kw = (keyword == null || keyword.trim().isEmpty()) ? null : keyword.trim();
        return promotionRepository.search(kw, status, pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionResponse getDetail(Long id) {
        return mapToResponse(findById(id));
    }

    @Override
    public PromotionResponse create(PromotionRequest request, String createdByUsername) {
        if (promotionRepository.existsByPromotionName(request.getPromotionName().trim())) {
            throw new ValidationException("Tên chương trình khuyến mãi đã tồn tại");
        }

        Employee employee = employeeRepository.findByUsername(createdByUsername)
                .orElseThrow(() -> new ValidationException("Không tìm thấy nhân viên: " + createdByUsername));

        List<PromotionDetail> details = buildAndValidateDetails(request.getDetails());

        Promotion promotion = Promotion.builder()
                .promotionName(request.getPromotionName().trim())
                .startDateTime(request.getStartDateTime())
                .endDateTime(request.getEndDateTime())
                .status(PromotionStatus.Draft)
                .createdBy(employee)
                .build();

        details.forEach(promotion::addDetail);
        return mapToResponse(promotionRepository.save(promotion));
    }

    @Override
    public PromotionResponse update(Long id, PromotionRequest request) {
        Promotion promotion = findById(id);

        if (promotion.getStatus() != PromotionStatus.Draft) {
            throw new ValidationException("Chỉ được phép chỉnh sửa chương trình ở trạng thái Bản nháp (Draft)");
        }

        if (promotionRepository.existsByPromotionNameAndPromotionIdNot(request.getPromotionName().trim(), id)) {
            throw new ValidationException("Tên chương trình khuyến mãi đã tồn tại");
        }

        List<PromotionDetail> details = buildAndValidateDetails(request.getDetails());

        promotion.setPromotionName(request.getPromotionName().trim());
        promotion.setStartDateTime(request.getStartDateTime());
        promotion.setEndDateTime(request.getEndDateTime());
        promotion.getDetails().clear();
        details.forEach(promotion::addDetail);

        return mapToResponse(promotionRepository.save(promotion));
    }

    @Override
    public void activate(Long id) {
        Promotion promotion = findById(id);
        if (promotion.getStatus() != PromotionStatus.Draft) {
            throw new ValidationException("Chỉ có thể kích hoạt chương trình ở trạng thái Bản nháp");
        }
        promotion.setStatus(PromotionStatus.Active);
        promotionRepository.save(promotion);
        logAudit("APPROVE_PROMOTION", "Promotion", id, PromotionStatus.Draft.name(), PromotionStatus.Active.name(), "Kích hoạt chương trình khuyến mãi");
    }

    @Override
    public void cancel(Long id) {
        Promotion promotion = findById(id);
        if (promotion.getStatus() == PromotionStatus.Canceled) {
            throw new ValidationException("Chương trình khuyến mãi đã bị hủy trước đó");
        }
        String oldStatus = promotion.getStatus().name();
        // Soft delete: chỉ cập nhật Status -> Canceled, không xóa vật lý
        promotion.setStatus(PromotionStatus.Canceled);
        promotionRepository.save(promotion);
        logAudit("CANCEL_PROMOTION", "Promotion", id, oldStatus, PromotionStatus.Canceled.name(), "Hủy chương trình khuyến mãi");
    }

    // ─── Private helpers ─────────────────────────────────────────────────────────

    private Promotion findById(Long id) {
        return promotionRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Không tìm thấy chương trình khuyến mãi với ID: " + id));
    }

    private List<PromotionDetail> buildAndValidateDetails(List<PromotionDetailRequest> detailRequests) {
        if (detailRequests == null || detailRequests.isEmpty()) {
            return new ArrayList<>();
        }

        // Kiểm tra sản phẩm trùng trong cùng request
        Set<Long> seen = new HashSet<>();
        for (PromotionDetailRequest dr : detailRequests) {
            if (dr.getProductId() == null) continue;
            if (!seen.add(dr.getProductId())) {
                throw new ValidationException("Sản phẩm ID=" + dr.getProductId()
                        + " bị trùng lặp trong cùng một chương trình khuyến mãi");
            }
        }

        List<PromotionDetail> result = new ArrayList<>();
        for (PromotionDetailRequest dr : detailRequests) {
            Product product = productRepository.findById(dr.getProductId())
                    .orElseThrow(() -> new ValidationException("Sản phẩm không tồn tại với ID: " + dr.getProductId()));

            DiscountType type = parseDiscountType(dr.getDiscountType());
            BigDecimal value  = dr.getDiscountValue();

            // Kiểm tra ràng buộc giá trị giảm theo loại
            if (type == DiscountType.Percentage) {
                if (value.compareTo(BigDecimal.ZERO) <= 0 || value.compareTo(new BigDecimal("100")) > 0) {
                    throw new ValidationException(
                            "Giảm giá theo phần trăm phải trong khoảng (0, 100] cho sản phẩm: " + product.getProductName());
                }
            } else {
                if (value.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new ValidationException(
                            "Giảm giá theo tiền mặt phải lớn hơn 0 cho sản phẩm: " + product.getProductName());
                }
            }

            result.add(PromotionDetail.builder()
                    .product(product)
                    .discountType(type)
                    .discountValue(value)
                    .build());
        }
        return result;
    }

    private DiscountType parseDiscountType(String raw) {
        try {
            return DiscountType.valueOf(raw);
        } catch (Exception e) {
            throw new ValidationException("Loại giảm giá không hợp lệ: " + raw);
        }
    }

    private PromotionResponse mapToResponse(Promotion p) {
        List<PromotionDetailResponse> details = p.getDetails() == null ? List.of() :
                p.getDetails().stream().map(d -> PromotionDetailResponse.builder()
                        .promotionDetailId(d.getPromotionDetailId())
                        .productId(d.getProduct().getProductId())
                        .productSku(d.getProduct().getSku())
                        .productName(d.getProduct().getProductName())
                        .discountType(d.getDiscountType())
                        .discountValue(d.getDiscountValue())
                        .build())
                .collect(Collectors.toList());

        return PromotionResponse.builder()
                .promotionId(p.getPromotionId())
                .promotionName(p.getPromotionName())
                .startDateTime(p.getStartDateTime())
                .endDateTime(p.getEndDateTime())
                .status(p.getStatus())
                .createdByName(p.getCreatedBy() != null ? p.getCreatedBy().getFullName() : "")
                .createdAt(p.getCreatedAt())
                .details(details)
                .build();
    }
}

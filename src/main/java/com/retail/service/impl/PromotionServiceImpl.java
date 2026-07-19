package com.retail.service.impl;

import com.retail.entity.Employee;
import com.retail.entity.Product;
import com.retail.entity.Promotion;
import com.retail.entity.PromotionDetail;
import com.retail.exception.ValidationException;
import com.retail.repository.PromotionDetailRepository;
import com.retail.repository.PromotionRepository;
import com.retail.service.PromotionService;
import com.retail.service.AuditLogService;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PromotionServiceImpl implements PromotionService {

    private final PromotionRepository promotionRepository;
    private final PromotionDetailRepository detailRepository;
    private final AuditLogService auditLogService;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public Promotion createPromotion(String name, String startStr, String endStr, Long createdByEmployeeId) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime start;
        LocalDateTime end;
        try {
            start = LocalDateTime.parse(startStr, formatter);
            end = LocalDateTime.parse(endStr, formatter);
        } catch (Exception e) {
            throw new ValidationException("Định dạng ngày bắt đầu hoặc kết thúc không hợp lệ (yêu cầu yyyy-MM-dd HH:mm:ss)");
        }

        if (end.isBefore(start) || end.isEqual(start)) {
            throw new ValidationException("Thời gian kết thúc phải sau thời gian bắt đầu");
        }

        Promotion promotion = Promotion.builder()
                .promotionName(name)
                .startDateTime(start)
                .endDateTime(end)
                .status("Draft")
                .createdBy(entityManager.getReference(Employee.class, createdByEmployeeId))
                .build();

        Promotion saved = promotionRepository.save(promotion);
        auditLogService.logAction(createdByEmployeeId, "CreatePromotion", "Promotion",
                saved.getPromotionId(), null, "Draft", "Tạo chương trình khuyến mãi nháp", null, null);
        return saved;
    }

    @Override
    @Transactional
    public Promotion addDetail(Long promotionId, Long productId, String discountType, BigDecimal discountValue) {
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new ValidationException("Khuyến mãi không tồn tại"));

        if (!"Draft".equals(promotion.getStatus())) {
            throw new ValidationException("Chỉ được chỉnh sửa chương trình khuyến mãi ở trạng thái Draft");
        }

        if (!"Percentage".equals(discountType) && !"FixedAmount".equals(discountType)) {
            throw new ValidationException("Loại giảm giá không hợp lệ (Percentage hoặc FixedAmount)");
        }

        if (discountValue.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Giá trị giảm giá không được âm");
        }

        if ("Percentage".equals(discountType) && discountValue.compareTo(new BigDecimal("100")) > 0) {
            throw new ValidationException("Phần trăm giảm giá không được vượt quá 100%");
        }

        Product product = entityManager.find(Product.class, productId);
        if (product == null) {
            throw new ValidationException("Sản phẩm không tồn tại");
        }

        PromotionDetail detail = PromotionDetail.builder()
                .promotion(promotion)
                .product(product)
                .discountType(discountType)
                .discountValue(discountValue)
                .build();

        promotion.addDetail(detail);
        return promotionRepository.save(promotion);
    }

    @Override
    @Transactional
    public Promotion activatePromotion(Long promotionId) {
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new ValidationException("Khuyến mãi không tồn tại"));

        if (!"Draft".equals(promotion.getStatus())) {
            throw new ValidationException("Chỉ chương trình ở trạng thái Draft mới được kích hoạt");
        }

        promotion.setStatus("Active");
        Promotion saved = promotionRepository.save(promotion);
        auditLogService.logAction(promotion.getCreatedBy().getEmployeeId(), "ActivatePromotion", "Promotion",
                saved.getPromotionId(), "Draft", "Active", "Kích hoạt chương trình khuyến mãi", null, null);
        return saved;
    }

    @Override
    @Transactional
    public Promotion cancelPromotion(Long promotionId) {
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new ValidationException("Khuyến mãi không tồn tại"));

        String oldStatus = promotion.getStatus();
        promotion.setStatus("Canceled");
        Promotion saved = promotionRepository.save(promotion);
        auditLogService.logAction(promotion.getCreatedBy().getEmployeeId(), "CancelPromotion", "Promotion",
                saved.getPromotionId(), oldStatus, "Canceled", "Hủy bỏ chương trình khuyến mãi", null, null);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionDetail getBestActivePromotion(Long productId, BigDecimal currentPrice) {
        List<PromotionDetail> details = detailRepository.findActivePromotionsForProduct(productId, LocalDateTime.now());
        if (details.isEmpty()) {
            return null;
        }

        PromotionDetail best = null;
        BigDecimal maxDiscount = BigDecimal.ZERO;

        for (PromotionDetail d : details) {
            BigDecimal discount = BigDecimal.ZERO;
            if ("Percentage".equals(d.getDiscountType())) {
                discount = currentPrice.multiply(d.getDiscountValue()).divide(new BigDecimal("100"));
            } else if ("FixedAmount".equals(d.getDiscountType())) {
                discount = d.getDiscountValue();
            }

            if (discount.compareTo(maxDiscount) > 0) {
                maxDiscount = discount;
                best = d;
            }
        }
        return best;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Promotion> getAllPromotions() {
        return promotionRepository.findAll();
    }
}

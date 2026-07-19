package com.retail.service;

import com.retail.dto.PromotionRequest;
import com.retail.dto.PromotionResponse;
import com.retail.entity.PromotionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PromotionService {
    Page<PromotionResponse> list(String keyword, PromotionStatus status, Pageable pageable);
    PromotionResponse getDetail(Long id);
    PromotionResponse create(PromotionRequest request, String createdByUsername);
    PromotionResponse update(Long id, PromotionRequest request);
    void activate(Long id);
    void cancel(Long id);
}

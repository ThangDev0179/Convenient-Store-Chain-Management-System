package com.retail.dto;

import com.retail.entity.PromotionStatus;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionResponse {
    private Long promotionId;
    private String promotionName;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private PromotionStatus status;
    private String createdByName;
    private LocalDateTime createdAt;
    private List<PromotionDetailResponse> details;
}

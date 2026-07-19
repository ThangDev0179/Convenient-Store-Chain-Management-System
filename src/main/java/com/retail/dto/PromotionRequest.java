package com.retail.dto;

import com.retail.validator.ValidDateRange;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ValidDateRange
public class PromotionRequest {

    @NotBlank(message = "Tên chương trình khuyến mãi không được để trống")
    @Size(max = 200, message = "Tên không được quá 200 ký tự")
    private String promotionName;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime startDateTime;

    @NotNull(message = "Ngày kết thúc không được để trống")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime endDateTime;

    @Builder.Default
    @Valid
    @NotEmpty(message = "Chương trình khuyến mãi phải áp dụng cho ít nhất 1 sản phẩm")
    private List<PromotionDetailRequest> details = new ArrayList<>();
}

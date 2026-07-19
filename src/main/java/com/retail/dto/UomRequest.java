package com.retail.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UomRequest {

    private Long id;

    @NotBlank(message = "Tên đơn vị tính không được để trống")
    @Size(max = 50, message = "Tên đơn vị tính không quá 50 ký tự")
    private String uomName;

    private Boolean isBaseUnit;

    @NotNull(message = "Tỉ lệ quy đổi không được để trống")
    @Min(value = 1, message = "Tỉ lệ quy đổi tối thiểu phải bằng 1")
    private Integer conversionRate;

    @Size(max = 100, message = "Mã vạch không được vượt quá 100 ký tự")
    private String barcode;

    @NotNull(message = "Giá bán tiêu chuẩn không được để trống")
    @PositiveOrZero(message = "Giá bán phải lớn hơn hoặc bằng 0")
    @Builder.Default
    private BigDecimal standardPrice = BigDecimal.ZERO;

    private String status;
}

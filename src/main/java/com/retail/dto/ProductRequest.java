package com.retail.dto;

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
public class ProductRequest {

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(max = 150, message = "Tên sản phẩm không quá 150 ký tự")
    private String productName;

    @Size(max = 100, message = "Mã vạch không được vượt quá 100 ký tự")
    private String barcode;

    private String description;

    @NotNull(message = "Danh mục không được để trống")
    private Integer categoryId;

    @NotNull(message = "Giá tiêu chuẩn không được để trống")
    @PositiveOrZero(message = "Giá tiêu chuẩn phải lớn hơn hoặc bằng 0")
    @Builder.Default
    private BigDecimal standardPrice = BigDecimal.ZERO;

    private Integer defaultSupplierId;

    @jakarta.validation.Valid
    @com.retail.validator.ValidUomList
    @Builder.Default
    private java.util.List<UomRequest> uoms = new java.util.ArrayList<>();
}

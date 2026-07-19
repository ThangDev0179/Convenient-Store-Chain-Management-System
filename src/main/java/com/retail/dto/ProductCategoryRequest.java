package com.retail.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductCategoryRequest {

    @NotBlank(message = "Tên ngành hàng không được để trống")
    @Size(max = 150, message = "Tên không vượt quá 150 ký tự")
    private String categoryName;

    @NotBlank(message = "Tiền tố SKU không được để trống")
    @Size(max = 5, message = "Tiền tố SKU tối đa 5 ký tự")
    @Pattern(regexp = "^[A-Z0-9]+$", message = "Tiền tố SKU chỉ chứa chữ in hoa và số, không khoảng trắng")
    private String skuPrefix;
}

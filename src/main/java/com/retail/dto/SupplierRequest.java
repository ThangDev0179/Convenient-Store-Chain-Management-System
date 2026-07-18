package com.retail.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierRequest {

    @NotBlank(message = "Tên nhà cung cấp không được để trống")
    private String supplierName;

    @Pattern(regexp = "^$|^(0|\\+84)[0-9]{9,10}$", message = "Số điện thoại không hợp lệ (phải bắt đầu bằng 0 hoặc +84 và có 10-11 chữ số)")
    private String contactPhone;

    @Email(message = "Email không đúng định dạng")
    private String contactEmail;

    private String address;
}

package com.retail.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SupplierRequest {

    @NotBlank(message = "Tên nhà cung cấp không được để trống")
    @Size(max = 200, message = "Tên nhà cung cấp không được vượt quá 200 ký tự")
    private String supplierName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(0|\\+84)[0-9]{9}$", message = "Số điện thoại không hợp lệ (phải gồm 10 chữ số bắt đầu bằng 0 hoặc +84)")
    @Size(max = 20, message = "Số điện thoại không quá 20 ký tự")
    private String contactPhone;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Địa chỉ email không đúng định dạng")
    @Size(max = 150, message = "Email không được vượt quá 150 ký tự")
    private String contactEmail;
    @Size(max = 300, message = "Địa chỉ không được vượt quá 300 ký tự")
    private String address;
}
package com.retail.dto;

import com.retail.entity.SupplierStatus;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierResponse {
    private Integer supplierId;
    private String supplierName;
    private String contactPhone;
    private String contactEmail;
    private String address;
    private SupplierStatus status;
    private LocalDateTime createdAt;
}

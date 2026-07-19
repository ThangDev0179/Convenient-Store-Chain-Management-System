package com.retail.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UomResponse {
    private Long id;
    private String uomName;
    private Boolean isBaseUnit;
    private Integer conversionRate;
    private String barcode;
    private BigDecimal standardPrice;
    private String status;
}

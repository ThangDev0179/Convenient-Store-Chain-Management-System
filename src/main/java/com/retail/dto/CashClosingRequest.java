package com.retail.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashClosingRequest {
    private Long workShiftId;
    private BigDecimal cashCounted;
    private BigDecimal bankCardAmount;
    private BigDecimal qrAmount;
}

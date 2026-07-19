package com.retail.entity;

import lombok.*;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BranchProductPriceId implements Serializable {
    private Integer branch;
    private Long product;
}

package com.retail.procurement;

import lombok.*;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BranchInventoryId implements Serializable {
    private Integer branch;
    private Long product;
}

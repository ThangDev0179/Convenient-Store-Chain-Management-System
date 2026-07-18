package com.retail.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BranchInventoryId implements Serializable {

    @Column(name = "BranchId")
    private Integer branchId;

    @Column(name = "ProductId")
    private Long productId;
}

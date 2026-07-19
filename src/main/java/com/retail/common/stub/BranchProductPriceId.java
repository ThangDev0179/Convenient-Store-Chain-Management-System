package com.retail.common.stub;

import java.io.Serializable;
import java.util.Objects;

public class BranchProductPriceId implements Serializable {
    private Integer branchId;
    private Long productId;

    public BranchProductPriceId() {}

    public BranchProductPriceId(Integer branchId, Long productId) {
        this.branchId = branchId;
        this.productId = productId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BranchProductPriceId that)) return false;
        return Objects.equals(branchId, that.branchId) && Objects.equals(productId, that.productId);
    }

    @Override
    public int hashCode() { return Objects.hash(branchId, productId); }
}

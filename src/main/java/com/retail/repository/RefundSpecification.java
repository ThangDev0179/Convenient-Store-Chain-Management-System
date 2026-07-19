package com.retail.repository;

import com.retail.entity.Refund;
import com.retail.entity.RefundStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class RefundSpecification {

    private RefundSpecification() {}

    public static Specification<Refund> buildFilter(RefundStatus status,
                                                    LocalDate fromDate,
                                                    LocalDate toDate,
                                                    Integer branchId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate.atStartOfDay()));
            }
            if (toDate != null) {
                predicates.add(cb.lessThan(root.get("createdAt"), toDate.plusDays(1).atStartOfDay()));
            }
            if (branchId != null) {
                predicates.add(cb.equal(root.get("branchId"), branchId));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

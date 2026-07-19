package com.retail.repository;

import com.retail.entity.Invoice;
import com.retail.entity.InvoiceStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic query builder for Invoice list/search endpoint.
 * Used with InvoiceRepository (extends JpaSpecificationExecutor).
 */
public class InvoiceSpecification {

    private InvoiceSpecification() {}

    /**
     * Build a Specification from optional filter parameters.
     *
     * @param status      filter by InvoiceStatus (nullable)
     * @param fromDate    filter CreatedAt >= startOfDay(fromDate) (nullable)
     * @param toDate      filter CreatedAt <= endOfDay(toDate) (nullable)
     * @param cashierId   filter by CashierId (nullable)
     * @param branchId    filter by BranchId (nullable — null means all branches, ADMIN/MANAGER)
     */
    public static Specification<Invoice> buildFilter(InvoiceStatus status,
                                                     LocalDate fromDate,
                                                     LocalDate toDate,
                                                     Long cashierId,
                                                     Integer branchId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"),
                        fromDate.atStartOfDay()));
            }
            if (toDate != null) {
                predicates.add(cb.lessThan(root.get("createdAt"),
                        toDate.plusDays(1).atStartOfDay()));
            }
            if (cashierId != null) {
                predicates.add(cb.equal(root.get("cashierId"), cashierId));
            }
            if (branchId != null) {
                predicates.add(cb.equal(root.get("branchId"), branchId));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

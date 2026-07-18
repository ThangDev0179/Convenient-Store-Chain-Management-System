package com.retail.repository;
import com.retail.entity.Branch;
import com.retail.entity.BranchStatus;
import com.retail.entity.Employee;
import com.retail.entity.WorkShift;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Integer>, JpaSpecificationExecutor<Branch> {
    Optional<Branch> findByBranchCode(String branchCode);
    boolean existsByBranchCode(String branchCode);
    long countByStatus(BranchStatus status);

    @Query(value = "SELECT COUNT(*) FROM [Employee] WHERE BranchId = :branchId AND Status = 'Active'", nativeQuery = true)
    long countActiveEmployees(@Param("branchId") Integer branchId);

    @Query(value = "SELECT COUNT(*) FROM [WorkShift] WHERE BranchId = :branchId AND Status = 'Open'", nativeQuery = true)
    long countOpenWorkShifts(@Param("branchId") Integer branchId);

    @Query(value = "SELECT COUNT(*) FROM [Invoice] WHERE BranchId = :branchId AND Status IN ('Draft', 'Held')", nativeQuery = true)
    long countOpenInvoices(@Param("branchId") Integer branchId);
}
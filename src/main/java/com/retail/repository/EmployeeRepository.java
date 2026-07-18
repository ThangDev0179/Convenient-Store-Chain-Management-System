package com.retail.repository;

import com.retail.entity.Employee;
import com.retail.entity.EmployeeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {
    Optional<Employee> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByEmailAndEmployeeIdNot(String email, Long employeeId);
    Optional<Employee> findByEmail(String email);

    @Query("SELECT COUNT(e) > 0 FROM Employee e WHERE e.branch.branchId = :branchId AND e.isBranchManager = true AND e.status = :status")
    boolean existsActiveBranchManager(@Param("branchId") Integer branchId, @Param("status") EmployeeStatus status);

    @Query("SELECT COUNT(e) FROM Employee e WHERE e.branch.branchId = :branchId AND e.isBranchManager = true AND e.status = :status")
    long countActiveBranchManagers(@Param("branchId") Integer branchId, @Param("status") EmployeeStatus status);

    @Query("SELECT MAX(e.employeeCode) FROM Employee e WHERE e.employeeCode LIKE :prefix%")
    String findMaxEmployeeCodeWithPrefix(@Param("prefix") String prefix);
}

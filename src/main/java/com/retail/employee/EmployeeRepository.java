package com.retail.employee;

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
    long countByStatus(EmployeeStatus status);

    @Query("SELECT COUNT(b) > 0 FROM Branch b WHERE b.branchId = :branchId AND b.manager IS NOT NULL AND b.manager.status = :status")
    boolean existsActiveBranchManager(@Param("branchId") Integer branchId, @Param("status") EmployeeStatus status);

    @Query("SELECT COUNT(b) FROM Branch b WHERE b.branchId = :branchId AND b.manager IS NOT NULL AND b.manager.status = :status")
    long countActiveBranchManagers(@Param("branchId") Integer branchId, @Param("status") EmployeeStatus status);

    @Query("SELECT MAX(e.employeeCode) FROM Employee e WHERE e.employeeCode LIKE :prefix%")
    String findMaxEmployeeCodeWithPrefix(@Param("prefix") String prefix);
}

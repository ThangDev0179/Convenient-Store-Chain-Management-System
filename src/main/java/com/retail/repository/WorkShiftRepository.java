package com.retail.repository;

import com.retail.entity.WorkShift;
import com.retail.entity.WorkShiftStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkShiftRepository extends JpaRepository<WorkShift, Long>, JpaSpecificationExecutor<WorkShift> {
    Optional<WorkShift> findFirstByEmployeeEmployeeIdAndStatus(Long employeeId, WorkShiftStatus status);
    boolean existsByEmployeeEmployeeIdAndStatus(Long employeeId, WorkShiftStatus status);
    long countByStatus(WorkShiftStatus status);
}

package com.retail.repository;

import com.retail.entity.EmployeeShiftSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EmployeeShiftScheduleRepository extends JpaRepository<EmployeeShiftSchedule, Long>, JpaSpecificationExecutor<EmployeeShiftSchedule> {

    @Query("SELECT s FROM EmployeeShiftSchedule s WHERE s.employee.employeeId = :employeeId " +
           "AND s.dayOfWeek = :dayOfWeek " +
           "AND (s.effectiveFrom <= :effectiveTo OR :effectiveTo IS NULL) " +
           "AND (s.effectiveTo >= :effectiveFrom OR s.effectiveTo IS NULL)")
    List<EmployeeShiftSchedule> findOverlappingSchedules(
            @Param("employeeId") Long employeeId,
            @Param("dayOfWeek") Integer dayOfWeek,
            @Param("effectiveFrom") LocalDate effectiveFrom,
            @Param("effectiveTo") LocalDate effectiveTo);

    @Query("SELECT s FROM EmployeeShiftSchedule s WHERE s.employee.employeeId = :employeeId " +
           "AND s.dayOfWeek = :dayOfWeek " +
           "AND s.scheduleId <> :scheduleId " +
           "AND (s.effectiveFrom <= :effectiveTo OR :effectiveTo IS NULL) " +
           "AND (s.effectiveTo >= :effectiveFrom OR s.effectiveTo IS NULL)")
    List<EmployeeShiftSchedule> findOverlappingSchedulesExcludingId(
            @Param("employeeId") Long employeeId,
            @Param("dayOfWeek") Integer dayOfWeek,
            @Param("effectiveFrom") LocalDate effectiveFrom,
            @Param("effectiveTo") LocalDate effectiveTo,
            @Param("scheduleId") Long scheduleId);
}

package com.retail.shift;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ShiftTypeRepository extends JpaRepository<ShiftType, Integer> {
    Optional<ShiftType> findByShiftName(String shiftName);
    boolean existsByShiftName(String shiftName);
    boolean existsByShiftNameAndShiftTypeIdNot(String shiftName, Integer shiftTypeId);
}

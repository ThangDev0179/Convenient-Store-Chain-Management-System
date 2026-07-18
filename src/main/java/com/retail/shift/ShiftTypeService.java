package com.retail.shift;

import com.retail.shift.dto.ShiftTypeRequest;
import java.util.List;

public interface ShiftTypeService {
    ShiftType create(ShiftTypeRequest request);
    ShiftType update(Integer shiftTypeId, ShiftTypeRequest request);
    void delete(Integer shiftTypeId);
    List<ShiftType> listAll();
    ShiftType getDetail(Integer shiftTypeId);
}

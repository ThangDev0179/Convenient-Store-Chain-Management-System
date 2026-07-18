package com.retail.service;
import com.retail.entity.ShiftType;
import com.retail.dto.ShiftTypeRequest;

import java.util.List;

public interface ShiftTypeService {
    ShiftType create(ShiftTypeRequest request);
    ShiftType update(Integer shiftTypeId, ShiftTypeRequest request);
    void delete(Integer shiftTypeId);
    List<ShiftType> listAll();
    ShiftType getDetail(Integer shiftTypeId);
}
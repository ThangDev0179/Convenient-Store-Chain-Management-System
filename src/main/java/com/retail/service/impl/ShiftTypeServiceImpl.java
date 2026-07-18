package com.retail.service.impl;
import com.retail.entity.ShiftType;
import com.retail.repository.ShiftTypeRepository;
import com.retail.dto.ShiftTypeRequest;
import com.retail.service.ShiftTypeService;
import com.retail.exception.ValidationException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class ShiftTypeServiceImpl implements ShiftTypeService {

    @Autowired
    private ShiftTypeRepository shiftTypeRepository;

    @Override
    public ShiftType create(ShiftTypeRequest request) {
        if (request.getShiftName() == null || request.getShiftName().trim().isEmpty()) {
            throw new ValidationException("Tên ca làm việc không được để trống");
        }
        if (request.getStartTime() == null || request.getEndTime() == null) {
            throw new ValidationException("Thời gian bắt đầu và kết thúc không được để trống");
        }

        String cleanedName = request.getShiftName().trim();
        if (shiftTypeRepository.existsByShiftName(cleanedName)) {
            throw new ValidationException("Tên ca làm việc đã tồn tại");
        }

        ShiftType shiftType = ShiftType.builder()
                .shiftName(cleanedName)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .build();

        return shiftTypeRepository.save(shiftType);
    }

    @Override
    public ShiftType update(Integer shiftTypeId, ShiftTypeRequest request) {
        ShiftType shiftType = shiftTypeRepository.findById(shiftTypeId)
                .orElseThrow(() -> new ValidationException("Ca làm việc không tồn tại"));

        if (request.getShiftName() == null || request.getShiftName().trim().isEmpty()) {
            throw new ValidationException("Tên ca làm việc không được để trống");
        }
        if (request.getStartTime() == null || request.getEndTime() == null) {
            throw new ValidationException("Thời gian bắt đầu và kết thúc không được để trống");
        }

        String cleanedName = request.getShiftName().trim();
        if (shiftTypeRepository.existsByShiftNameAndShiftTypeIdNot(cleanedName, shiftTypeId)) {
            throw new ValidationException("Tên ca làm việc đã tồn tại");
        }

        shiftType.setShiftName(cleanedName);
        shiftType.setStartTime(request.getStartTime());
        shiftType.setEndTime(request.getEndTime());

        return shiftTypeRepository.save(shiftType);
    }

    @Override
    public void delete(Integer shiftTypeId) {
        if (!shiftTypeRepository.existsById(shiftTypeId)) {
            throw new ValidationException("Ca làm việc không tồn tại");
        }
        shiftTypeRepository.deleteById(shiftTypeId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShiftType> listAll() {
        return shiftTypeRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public ShiftType getDetail(Integer shiftTypeId) {
        return shiftTypeRepository.findById(shiftTypeId)
                .orElseThrow(() -> new ValidationException("Ca làm việc không tồn tại"));
    }
}
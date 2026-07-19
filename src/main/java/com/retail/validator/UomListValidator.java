package com.retail.validator;

import com.retail.dto.UomRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class UomListValidator implements ConstraintValidator<ValidUomList, List<UomRequest>> {

    @Override
    public boolean isValid(List<UomRequest> uoms, ConstraintValidatorContext context) {
        if (uoms == null || uoms.isEmpty()) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Sản phẩm phải có ít nhất 1 đơn vị tính (UOM)")
                   .addConstraintViolation();
            return false;
        }

        // 1. Phải có chính xác 1 UOM là Base Unit (is_base_unit = true)
        long baseUnitCount = uoms.stream()
                .filter(u -> u.getIsBaseUnit() != null && u.getIsBaseUnit())
                .count();
        if (baseUnitCount != 1) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Sản phẩm phải có chính xác 1 đơn vị tính cơ sở (Base Unit)")
                   .addConstraintViolation();
            return false;
        }

        // Tìm Base Unit
        UomRequest baseUnit = uoms.stream()
                .filter(u -> u.getIsBaseUnit() != null && u.getIsBaseUnit())
                .findFirst()
                .orElse(null);

        // 2. Tỷ lệ quy đổi của Base Unit bắt buộc = 1
        if (baseUnit != null && (baseUnit.getConversionRate() == null || baseUnit.getConversionRate() != 1)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Tỉ lệ quy đổi của đơn vị tính cơ sở bắt buộc phải bằng 1")
                   .addConstraintViolation();
            return false;
        }

        // 3. Tỷ lệ quy đổi của các UOM khác bắt buộc > 1
        for (UomRequest u : uoms) {
            if (u.getIsBaseUnit() == null || !u.getIsBaseUnit()) {
                if (u.getConversionRate() == null || u.getConversionRate() <= 1) {
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate("Tỉ lệ quy đổi của đơn vị quy đổi khác bắt buộc phải lớn hơn 1")
                           .addConstraintViolation();
                    return false;
                }
            }
        }

        // 4. Không được có 2 UOM nào trùng tên (uom_name) trong cùng danh sách (không phân biệt hoa thường)
        Set<String> uomNames = uoms.stream()
                .map(u -> u.getUomName() != null ? u.getUomName().trim().toLowerCase() : "")
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toSet());

        long validNamesCount = uoms.stream()
                .map(u -> u.getUomName() != null ? u.getUomName().trim().toLowerCase() : "")
                .filter(name -> !name.isEmpty())
                .count();

        if (uomNames.size() < uoms.size() || validNamesCount < uoms.size()) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Tên các đơn vị tính không được để trống hoặc trùng lặp")
                   .addConstraintViolation();
            return false;
        }

        return true;
    }
}

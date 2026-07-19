package com.retail.validator;

import com.retail.dto.PromotionRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DateRangeValidator implements ConstraintValidator<ValidDateRange, PromotionRequest> {

    @Override
    public boolean isValid(PromotionRequest request, ConstraintValidatorContext context) {
        if (request == null) return true;
        if (request.getStartDateTime() == null || request.getEndDateTime() == null) return true;
        boolean valid = request.getEndDateTime().isAfter(request.getStartDateTime());
        if (!valid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Ngày kết thúc phải lớn hơn ngày bắt đầu")
                   .addPropertyNode("endDateTime")
                   .addConstraintViolation();
        }
        return valid;
    }
}

package com.retail.promotion.validation;

import com.retail.dto.PromotionRequest;
import com.retail.validator.DateRangeValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class DateRangeValidatorTest {

    private DateRangeValidator validator;
    private ConstraintValidatorContext context;
    private ConstraintValidatorContext.ConstraintViolationBuilder builder;
    private ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext nodeContext;

    @BeforeEach
    void setUp() {
        validator = new DateRangeValidator();
        context = Mockito.mock(ConstraintValidatorContext.class);
        builder = Mockito.mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        nodeContext = Mockito.mock(ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext.class);

        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(builder);
        when(builder.addPropertyNode(anyString())).thenReturn(nodeContext);
    }

    @Test
    void shouldReturnTrueWhenRequestIsNull() {
        boolean result = validator.isValid(null, context);
        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnTrueWhenStartOrEndIsNull() {
        PromotionRequest request1 = PromotionRequest.builder()
                .startDateTime(null)
                .endDateTime(LocalDateTime.now())
                .build();
        PromotionRequest request2 = PromotionRequest.builder()
                .startDateTime(LocalDateTime.now())
                .endDateTime(null)
                .build();

        assertThat(validator.isValid(request1, context)).isTrue();
        assertThat(validator.isValid(request2, context)).isTrue();
    }

    @Test
    void shouldReturnTrueWhenEndIsAfterStart() {
        PromotionRequest request = PromotionRequest.builder()
                .startDateTime(LocalDateTime.now())
                .endDateTime(LocalDateTime.now().plusDays(1))
                .build();

        assertThat(validator.isValid(request, context)).isTrue();
    }

    @Test
    void shouldReturnFalseWhenEndIsBeforeStart() {
        PromotionRequest request = PromotionRequest.builder()
                .startDateTime(LocalDateTime.now())
                .endDateTime(LocalDateTime.now().minusDays(1))
                .build();

        assertThat(validator.isValid(request, context)).isFalse();
    }

    @Test
    void shouldReturnFalseWhenEndIsEqualToStart() {
        LocalDateTime time = LocalDateTime.now();
        PromotionRequest request = PromotionRequest.builder()
                .startDateTime(time)
                .endDateTime(time)
                .build();

        assertThat(validator.isValid(request, context)).isFalse();
    }
}

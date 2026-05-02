package com.linan.core.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * {@link ValueInEnum} 注解的校验器
 */
public class ValueInEnumValidator implements ConstraintValidator<ValueInEnum, String> {

    private Set<String> allowedValues;

    @Override
    public void initialize(ValueInEnum annotation) {
        allowedValues = Arrays.stream(annotation.value())
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // null 值由 @NotBlank 或其他注解校验
        if (value == null || value.isEmpty()) {
            return true;
        }
        return allowedValues.contains(value);
    }
}

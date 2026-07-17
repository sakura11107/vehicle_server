package com.vehicle.server.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDateTime;

public class NotTooFarPastValidator implements ConstraintValidator<NotTooFarPast, LocalDateTime> {

    private int toleranceMinutes;

    @Override
    public void initialize(NotTooFarPast constraintAnnotation) {
        this.toleranceMinutes = constraintAnnotation.toleranceMinutes();
    }

    @Override
    public boolean isValid(LocalDateTime value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return !value.isBefore(LocalDateTime.now().minusMinutes(toleranceMinutes));
    }
}

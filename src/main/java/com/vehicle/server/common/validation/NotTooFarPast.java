package com.vehicle.server.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 校验时间不能太久以前，默认容忍30分钟。
 * 用于预约 startTime，覆盖前端选时→提交的网络延迟。
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = NotTooFarPastValidator.class)
public @interface NotTooFarPast {

    String message() default "不能太久以前，最多允许30分钟前";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    int toleranceMinutes() default 30;
}

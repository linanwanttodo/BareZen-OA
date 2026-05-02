package com.linan.core.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * 自定义校验注解：检查字段值是否在指定的枚举值集合中
 *
 * <p>使用方式：</p>
 * <pre>{@code
 * @ValueInEnum({"0", "1"})
 * private String status;
 * }</pre>
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValueInEnumValidator.class)
@Documented
public @interface ValueInEnum {

    String message() default "字段值不在允许的范围内";

    String[] value();

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

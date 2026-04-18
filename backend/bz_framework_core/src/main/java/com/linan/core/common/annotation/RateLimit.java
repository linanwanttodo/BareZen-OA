package com.linan.core.common.annotation;

import java.lang.annotation.*;

/** 接口限流注解 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    String key() default "";
    int limit() default 5;
    int window() default 60;
}
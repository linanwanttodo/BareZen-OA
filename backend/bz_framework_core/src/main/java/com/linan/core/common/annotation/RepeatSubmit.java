package com.linan.core.common.annotation;

import java.lang.annotation.*;

/** 防重复提交注解 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RepeatSubmit {
    int interval() default 3;
}
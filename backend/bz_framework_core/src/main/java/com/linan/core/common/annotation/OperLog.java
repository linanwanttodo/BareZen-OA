package com.linan.core.common.annotation;

import java.lang.annotation.*;

/**
 * 自定义操作日志记录注解
 *
 * 使用方式：
 * @OperLog(title = "用户管理", businessType = BusinessType.INSERT)
 * public Result saveUser(...) { ... }
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperLog {
    /**
     * 模块标题（例如：用户管理、角色管理）
     */
    String title() default "";

    /**
     * 业务类型（0=其它, 1=新增, 2=修改, 3=删除）
     */
    int businessType() default 0;
}

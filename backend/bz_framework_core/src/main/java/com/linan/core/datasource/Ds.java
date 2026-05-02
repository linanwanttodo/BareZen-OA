package com.linan.core.datasource;

import java.lang.annotation.*;

/**
 * 动态数据源切换注解
 *
 * <p>标注在方法或类上，用于指定该方法/类中所有数据库操作使用的数据源。
 * 默认使用 "master" 数据源。可通过 value 属性指定其他数据源名称（如 "slave"）。</p>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * @Ds("slave")
 * public List<User> listUsers() {
 *     return userMapper.selectList(null);
 * }
 * }</pre>
 *
 * <p>注意：该注解需要配合 {@link DsAspect} 切面使用才能生效。
 * 优先级高于事务注解（通过 {@link org.springframework.core.annotation.Order} 控制）。</p>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Ds {

    /**
     * 数据源名称
     * <p>对应 DynamicDataSource 中 targetDataSources map 的 key。</p>
     */
    String value() default "master";
}

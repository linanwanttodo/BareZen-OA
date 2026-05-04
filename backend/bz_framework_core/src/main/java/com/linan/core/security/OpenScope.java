package com.linan.core.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 开放平台接口权限范围注解
 *
 * <p>标注在开放接口（/api/open/**）方法上，声明该方法所需的 API Key 权限范围
 * （如 {@code open:approval:read}）。由切面校验当前请求的 API Key scopes
 * 是否包含该范围（支持 {@code *} 通配）。</p>
 *
 * @author BareZen Team
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OpenScope {

    /** 所需权限范围，如 open:approval:read */
    String value();
}

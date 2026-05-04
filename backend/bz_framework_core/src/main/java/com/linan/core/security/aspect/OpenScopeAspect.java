package com.linan.core.security.aspect;

import com.linan.core.common.api.Result;
import com.linan.core.common.api.ResultCodeEnum;
import com.linan.core.security.OpenScope;
import com.linan.core.security.interceptor.OpenApiInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 开放接口权限范围校验切面
 *
 * <p>校验 {@code @OpenScope} 注解声明的权限范围是否在当前请求的 API Key scopes 中
 * （支持 {@code *} 通配）。仅对 /api/open/** 的开放接口生效。</p>
 *
 * @author BareZen Team
 */
@Slf4j
@Aspect
@Component
public class OpenScopeAspect {

    @Around("@annotation(com.linan.core.security.OpenScope)")
    public Object checkScope(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;
        if (request == null) {
            return Result.error(ResultCodeEnum.UNAUTHORIZED);
        }

        // 拦截器已把校验通过的 API Key scopes 写入 request 属性
        Object scopesAttr = request.getAttribute(OpenApiInterceptor.ATTR_SCOPES);
        if (!(scopesAttr instanceof String scopes) || scopes.isBlank()) {
            return Result.error(ResultCodeEnum.FORBIDDEN);
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        OpenScope openScope = method.getAnnotation(OpenScope.class);
        String required = openScope.value();

        Set<String> granted = Arrays.stream(scopes.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        if (granted.contains("*") || granted.contains(required)) {
            return joinPoint.proceed();
        }

        log.warn("Open API 权限不足: 需要 {}, 实际 {}", required, scopes);
        return Result.error(ResultCodeEnum.FORBIDDEN);
    }
}

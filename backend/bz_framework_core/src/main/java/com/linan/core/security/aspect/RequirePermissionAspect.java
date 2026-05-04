package com.linan.core.security.aspect;

import com.linan.core.common.api.Result;
import com.linan.core.common.api.ResultCodeEnum;
import com.linan.core.security.PermissionService;
import com.linan.core.security.RequirePermission;
import com.linan.core.security.interceptor.AuthInterceptor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * AOP 权限校验切面
 *
 * <p>拦截所有标记了 @RequirePermission 注解的 Controller 方法，
 * 在执行前通过 {@link PermissionService} 检查当前用户是否拥有所需的权限标识。</p>
 *
 * <p>使用 {@link PermissionService#getUserPermissions} 查询已缓存的权限集合，
 * 避免每次请求都重复查询数据库。</p>
 *
 * <p>使用方式：</p>
 * <pre>{@code
 * @RequirePermission("system:user:delete")                     // 单权限
 * @RequirePermission({"system:user:add", "system:user:edit"})  // 多权限 AND
 * }</pre>
 *
 * @author BareZen Team
 */
@Aspect
@Component
@RequiredArgsConstructor
public class RequirePermissionAspect {

    private final PermissionService permissionService;

    /**
     * 切入点：所有标记了 @RequirePermission 的方法
     */
    @Pointcut("@annotation(com.linan.core.security.RequirePermission)")
    public void pointcut() {}

    /**
     * 环绕通知
     *
     * 在目标方法执行前进行权限校验。
     * 使用 PermissionService 的缓存能力，避免 N+1 数据库查询。
     */
    @Around("pointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 1. 获取当前用户 ID
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("未登录");
        }

        // 2. 获取方法上的 @RequirePermission 注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        RequirePermission annotation = signature.getMethod()
                .getAnnotation(RequirePermission.class);
        if (annotation == null) {
            return joinPoint.proceed();
        }

        // 3. 通过 PermissionService（含缓存）检查权限
        String[] requiredPerms = annotation.value();
        boolean hasPermission = permissionService.hasAllPermissions(userId, requiredPerms);

        if (hasPermission) {
            return joinPoint.proceed();
        } else {
            return new Result<>(ResultCodeEnum.FORBIDDEN.getCode(), "没有访问权限，请联系管理员", null);
        }
    }
}

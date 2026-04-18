package com.linan.core.security.aspect;

import com.linan.core.common.api.Result;
import com.linan.core.security.RequirePermission;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.system.entity.SysRole;
import com.linan.system.mapper.SysMenuMapper;
import com.linan.system.mapper.SysRoleMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * AOP 权限校验切面
 *
 * 拦截所有标记了 @RequirePermission 注解的 Controller 方法，
 * 在执行前检查当前用户是否拥有所需的权限标识。
 *
 * 执行流程：
 * 1. 从 AuthInterceptor 的 ThreadLocal 获取当前用户ID
 * 2. 查询用户的所有角色
 * 3. 检查是否为 admin 角色（超级管理员直接放行）
 * 4. 收集所有角色的权限标识集合
 * 5. 检查是否包含方法要求的权限标识（AND 关系）
 * 6. 有权限 -> 放行；无权限 -> 返回 403
 *
 * 使用方式：
 * @RequirePermission("system:user:delete")          // 单权限
 * @RequirePermission({"system:user:add", "system:user:edit"})  // 多权限 AND
 *
 * @author BareZen Team
 * @since 1.0-SNAPSHOT
 */
@Aspect
@Component
public class RequirePermissionAspect {

    @Autowired
    private SysRoleMapper roleMapper;

    @Autowired
    private SysMenuMapper menuMapper;

    /**
     * 切入点：所有标记了 @RequirePermission 的方法
     */
    @Pointcut("@annotation(com.linan.core.security.RequirePermission)")
    public void pointcut() {}

    /**
     * 环绕通知
     *
     * 在目标方法执行前进行权限校验。
     * 如果用户没有所需权限，直接返回 403 错误，不执行目标方法。
     *
     * @param joinPoint 连接点
     * @return 目标方法的返回值（有权限）或 403 错误（无权限）
     */
    @Around("pointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 1. 获取当前用户 ID（AuthInterceptor 在 Token 解析后存入 ThreadLocal）
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            // 理论上不会走到这里（AuthInterceptor 会先拦截未登录请求）
            return Result.error("未登录");
        }

        // 2. 获取方法上的 @RequirePermission 注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        RequirePermission annotation = signature.getMethod()
                .getAnnotation(RequirePermission.class);
        if (annotation == null) {
            return joinPoint.proceed();  // 无注解，放行
        }

        // 3. 获取方法要求的权限标识数组
        String[] requiredPerms = annotation.value();

        // 4. 查询当前用户的所有角色（三表联查：sys_user_role + sys_role）
        List<SysRole> roles = roleMapper.selectRolesByUserId(userId);

        // 5. 检查是否为 admin 角色（超级管理员，直接放行）
        //    这是业界通用做法：admin 角色不需要配置权限标识，代码中直接放行
        boolean isAdmin = roles.stream()
                .anyMatch(r -> "admin".equals(r.getRoleKey()));
        if (isAdmin) {
            return joinPoint.proceed();
        }

        // 6. 收集用户所有权限标识（遍历角色，通过角色ID查菜单权限）
        Set<String> userPerms = new HashSet<>();
        for (SysRole role : roles) {
            // selectPermsByRoleId：联表查询 sys_role_menu + sys_menu
            List<String> perms = menuMapper.selectPermsByRoleId(role.getId());
            userPerms.addAll(perms);  // Set 自动去重
        }

        // 7. 检查用户是否拥有所有要求的权限（AND 关系）
        //    例：@RequirePermission({"system:user:add", "system:user:edit"})
        //    用户必须同时拥有 add 和 edit 权限
        boolean hasPermission = Arrays.stream(requiredPerms)
                .allMatch(userPerms::contains);

        if (hasPermission) {
            return joinPoint.proceed();  // 有权限，放行
        } else {
            return Result.error("没有访问权限，请联系管理员");
        }
    }
}
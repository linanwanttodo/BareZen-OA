package com.linan.core.security.aspect;

import com.linan.core.common.api.Result;
import com.linan.core.common.api.ResultCodeEnum;
import com.linan.core.security.PermissionService;
import com.linan.core.security.RequirePermission;
import com.linan.core.security.interceptor.AuthInterceptor;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RequirePermissionAspect 单元测试
 *
 * <p>覆盖 T16 语义：无权限返回真实 HTTP 403 状态码（body 仍为统一 Result code=403）、
 * 未登录拦截、有权限放行、无 RequestContext 时不抛异常。</p>
 */
@ExtendWith(MockitoExtension.class)
class RequirePermissionAspectTest {

    @Mock
    private PermissionService permissionService;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature signature;

    @Mock
    private ServletRequestAttributes requestAttributes;

    @Mock
    private HttpServletResponse response;

    private RequirePermissionAspect aspect;

    @RequirePermission("test:perm")
    private void stubMethod() {
        // 仅用于通过反射获取带 @RequirePermission 的方法
    }

    @BeforeEach
    void setUp() {
        aspect = new RequirePermissionAspect(permissionService);
        RequestContextHolder.setRequestAttributes(requestAttributes);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        AuthInterceptor.clearCurrentUser();
    }

    /** 反射注入当前登录用户（AuthInterceptor 的 ThreadLocal 无公开 setter） */
    private static void setCurrentUser(Long userId) throws Exception {
        Field field = AuthInterceptor.class.getDeclaredField("currentUserIdHolder");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        ThreadLocal<Long> tl = (ThreadLocal<Long>) field.get(null);
        tl.set(userId);
    }

    /** stub 注解查找链路（joinPoint -> signature -> method） */
    private void stubAnnotationLookup() throws Exception {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(
                RequirePermissionAspectTest.class.getDeclaredMethod("stubMethod"));
    }

    @Test
    @DisplayName("未登录返回业务错误，不触达业务方法，不设置 403")
    void notLoggedIn_returnsError() throws Throwable {
        Result<?> result = (Result<?>) aspect.around(joinPoint);

        assertEquals(500, result.getCode());
        verify(joinPoint, never()).proceed();
        verify(response, never()).setStatus(any(int.class));
    }

    @Test
    @DisplayName("有权限放行并返回业务方法结果")
    void hasPermission_proceeds() throws Throwable {
        setCurrentUser(1L);
        stubAnnotationLookup();
        when(permissionService.hasAllPermissions(anyLong(), any())).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("ok");

        Object result = aspect.around(joinPoint);

        assertEquals("ok", result);
        verify(joinPoint).proceed();
        verify(response, never()).setStatus(any(int.class));
    }

    @Test
    @DisplayName("无权限：HTTP 状态码 403 + 统一 Result code=403，不触达业务方法")
    void noPermission_returnsForbidden() throws Throwable {
        setCurrentUser(1L);
        stubAnnotationLookup();
        when(permissionService.hasAllPermissions(anyLong(), any())).thenReturn(false);
        when(requestAttributes.getResponse()).thenReturn(response);

        Result<?> result = (Result<?>) aspect.around(joinPoint);

        assertEquals(ResultCodeEnum.FORBIDDEN.getCode(), result.getCode());
        assertEquals(403, result.getCode());
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(joinPoint, never()).proceed();
    }

    @Test
    @DisplayName("无 RequestContext（非 HTTP 调用）时不抛异常，仅返回 Result 403")
    void noRequestContext_returnsForbiddenWithoutException() throws Throwable {
        setCurrentUser(1L);
        stubAnnotationLookup();
        when(permissionService.hasAllPermissions(anyLong(), any())).thenReturn(false);
        RequestContextHolder.resetRequestAttributes();

        Result<?> result = (Result<?>) aspect.around(joinPoint);

        assertEquals(ResultCodeEnum.FORBIDDEN.getCode(), result.getCode());
        verify(joinPoint, never()).proceed();
    }
}
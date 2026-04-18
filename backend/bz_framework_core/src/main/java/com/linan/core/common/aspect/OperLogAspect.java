package com.linan.core.common.aspect;

import com.linan.core.common.annotation.OperLog;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.system.entity.SysOperLog;
import com.linan.system.service.ISysOperLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * 操作日志 AOP 切面
 *
 * 负责拦截 @OperLog 注解，记录用户的操作行为到 sys_oper_log 表。
 */
@Slf4j
@Aspect
@Component
public class OperLogAspect {

    @Autowired
    private ISysOperLogService operLogService;

    @Around("@annotation(com.linan.core.common.annotation.OperLog)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        OperLog operLog = method.getAnnotation(OperLog.class);

        SysOperLog logEntity = new SysOperLog();
        logEntity.setTitle(operLog.title());
        logEntity.setBusinessType(operLog.businessType());
        logEntity.setClassName(joinPoint.getTarget().getClass().getName());
        logEntity.setMethodName(method.getName());

        if (request != null) {
            logEntity.setRequestMethod(request.getMethod());
            logEntity.setOperUrl(request.getRequestURI());
            logEntity.setOperIp(getClientIp(request));
        }

        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId != null) {
            logEntity.setOperUser(userId.toString());
        }

        Object result = null;
        try {
            result = joinPoint.proceed();
            logEntity.setStatus(0);
        } catch (Exception e) {
            logEntity.setStatus(1);
            logEntity.setErrorMsg(e.getMessage());
            throw e;
        } finally {
            long endTime = System.currentTimeMillis();
            logEntity.setCostTime(endTime - startTime);
            try {
                operLogService.save(logEntity);
            } catch (Exception e) {
                log.error("保存操作日志失败", e);
            }
        }

        return result;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}

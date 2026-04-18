package com.linan.core.common.aspect;

import com.linan.core.common.annotation.RepeatSubmit;
import com.linan.core.common.api.Result;
import com.linan.core.security.interceptor.AuthInterceptor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
public class RepeatSubmitAspect {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String REPEAT_SUBMIT_PREFIX = "repeat_submit:";

    @Pointcut("@annotation(com.linan.core.common.annotation.RepeatSubmit)")
    public void pointcut() {}

    @Around("pointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        RepeatSubmit repeatSubmit = signature.getMethod().getAnnotation(RepeatSubmit.class);

        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = getHttpServletRequest();
        String fingerprint = generateFingerprint(request, userId, joinPoint.getArgs());
        String key = REPEAT_SUBMIT_PREFIX + fingerprint;

        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(key, "1", repeatSubmit.interval(), TimeUnit.SECONDS);

        if (Boolean.TRUE.equals(acquired)) {
            return joinPoint.proceed();
        }

        return Result.error("请勿重复提交");
    }

    private String generateFingerprint(HttpServletRequest request, Long userId, Object[] args) {
        StringBuilder sb = new StringBuilder();
        sb.append(userId).append(":");
        if (request != null) {
            sb.append(request.getRequestURI());
        }
        if (args != null) {
            for (Object arg : args) {
                if (arg != null && !(arg instanceof org.springframework.web.multipart.MultipartFile)) {
                    sb.append(arg.toString());
                }
            }
        }
        return DigestUtils.md5DigestAsHex(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private HttpServletRequest getHttpServletRequest() {
        try {
            org.springframework.web.context.request.ServletRequestAttributes attrs =
                    (org.springframework.web.context.request.ServletRequestAttributes)
                    org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return null;
            }
            return attrs.getRequest();
        } catch (Exception e) {
            return null;
        }
    }
}

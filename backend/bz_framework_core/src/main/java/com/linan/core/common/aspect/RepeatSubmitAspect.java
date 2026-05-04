package com.linan.core.common.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linan.core.common.annotation.RepeatSubmit;
import com.linan.core.common.api.Result;
import com.linan.core.common.util.IpUtil;
import com.linan.core.security.interceptor.AuthInterceptor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
@RequiredArgsConstructor
public class RepeatSubmitAspect {

    private final RedisTemplate<String, Object> redisTemplate;

    private final IpUtil ipUtil;

    private static final String REPEAT_SUBMIT_PREFIX = "repeat_submit:";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Pointcut("@annotation(com.linan.core.common.annotation.RepeatSubmit)")
    public void pointcut() {}

    @Around("pointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        RepeatSubmit repeatSubmit = signature.getMethod().getAnnotation(RepeatSubmit.class);

        // 获取用户ID（登录用户）或 IP（匿名用户）
        Long userId = AuthInterceptor.getCurrentUserId();
        String identifier;
        if (userId != null) {
            identifier = "user:" + userId;
        } else {
            // 匿名请求：使用客户端 IP 作为标识
            HttpServletRequest request = getHttpServletRequest();
            if (request != null) {
                identifier = "ip:" + ipUtil.getClientIp(request);
            } else {
                return joinPoint.proceed(); // 无法获取标识，放行
            }
        }

        String fingerprint = generateFingerprint(identifier, joinPoint.getArgs());
        String key = REPEAT_SUBMIT_PREFIX + fingerprint;

        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(key, "1", repeatSubmit.interval(), TimeUnit.SECONDS);

        if (Boolean.TRUE.equals(acquired)) {
            return joinPoint.proceed();
        }

        return Result.error("请勿重复提交");
    }

    /**
     * 生成请求指纹：标识符 + URI + 参数 JSON 序列化
     */
    private String generateFingerprint(String identifier, Object[] args) {
        StringBuilder sb = new StringBuilder();
        sb.append(identifier).append(":");

        HttpServletRequest request = getHttpServletRequest();
        if (request != null) {
            sb.append(request.getRequestURI());
        }

        // 使用 JSON 序列化参数，替代不可靠的 toString()
        if (args != null) {
            for (Object arg : args) {
                if (arg != null && !(arg instanceof org.springframework.web.multipart.MultipartFile)) {
                    try {
                        String json = objectMapper.writeValueAsString(arg);
                        sb.append(json);
                    } catch (Exception e) {
                        sb.append(arg.toString());
                    }
                }
            }
        }

        // 使用 SHA-256 生成定长指纹
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // fallback to MD5 (should not happen on standard JVM)
            return org.springframework.util.DigestUtils.md5DigestAsHex(
                    sb.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    private HttpServletRequest getHttpServletRequest() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return null;
            }
            return attrs.getRequest();
        } catch (Exception e) {
            return null;
        }
    }
}

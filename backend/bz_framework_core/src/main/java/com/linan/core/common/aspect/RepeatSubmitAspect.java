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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeUnit;

@Slf4j
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
            // 业务执行异常时必须释放防重 key，否则 interval 内用户重试会被误拦；
            // 成功路径保持原有 TTL 语义（key 自然过期），故只在异常分支删除
            try {
                return joinPoint.proceed();
            } catch (Throwable e) {
                releaseKey(key);
                throw e;
            }
        }

        return Result.error("请勿重复提交");
    }

    /**
     * 删除防重 key（异常分支回滚用），删除失败仅记录日志不影响主流程
     */
    private void releaseKey(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("释放防重提交 key 失败: {}", key, e);
        }
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

        // 使用 SHA-256 生成定长指纹（统一走 DigestUtil）
        return com.linan.core.common.util.DigestUtil.sha256Hex(
                sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
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

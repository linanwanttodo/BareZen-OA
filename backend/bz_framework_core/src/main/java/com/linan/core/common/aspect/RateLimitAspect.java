package com.linan.core.common.aspect;

import com.linan.core.common.annotation.RateLimit;
import com.linan.core.common.api.Result;
import com.linan.core.common.util.IpUtil;
import com.linan.core.security.interceptor.AuthInterceptor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final StringRedisTemplate stringRedisTemplate;

    private final IpUtil ipUtil;

    private static final String RATE_LIMIT_PREFIX = "rate_limit:";

    /**
     * Lua 脚本：固定窗口计数器，每次调用都重新设置 TTL 防止 key 永久存在。
     *
     * KEYS[1] = 限流 key
     * ARGV[1] = 窗口内最大请求数
     * ARGV[2] = 窗口大小（秒）
     * 返回值：1=允许 0=拒绝
     */
    private static final DefaultRedisScript<Long> RATE_LIMIT_SCRIPT = new DefaultRedisScript<>(
            "local key = KEYS[1] " +
            "local limit = tonumber(ARGV[1]) " +
            "local window = tonumber(ARGV[2]) " +
            "local current = redis.call('INCR', key) " +
            "if current == 1 then " +
            "    redis.call('EXPIRE', key, window) " +
            "end " +
            "if current > limit then return 0 else return 1 end",
            Long.class
    );

    @Pointcut("@annotation(com.linan.core.common.annotation.RateLimit)")
    public void pointcut() {}

    @Around("pointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        RateLimit rateLimit = signature.getMethod().getAnnotation(RateLimit.class);

        String key = buildKey(rateLimit);
        String fullKey = RATE_LIMIT_PREFIX + key;

        Long result = stringRedisTemplate.execute(
                RATE_LIMIT_SCRIPT,
                List.of(fullKey),
                String.valueOf(rateLimit.limit()),
                String.valueOf(rateLimit.window())
        );
        if (result == null || result == 0) {
            return Result.error("请求过于频繁，请稍后再试");
        }

        return joinPoint.proceed();
    }

    private String buildKey(RateLimit rateLimit) {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "unknown";
        }

        HttpServletRequest request = attributes.getRequest();
        String ip = ipUtil.getClientIp(request);

        if (rateLimit.key() != null && !rateLimit.key().isEmpty()) {
            // 固定 key 追加维度：已登录用户按用户维度、匿名请求按 IP 维度，
            // 避免「auth:login」这类全局窗口被单点耗尽导致误伤全员（或全员共享同一配额）
            Long userId = AuthInterceptor.getCurrentUserId();
            String dimension = userId != null ? "user:" + userId : "ip:" + ip;
            return rateLimit.key() + ":" + dimension;
        }

        String method = request.getMethod();
        String path = request.getRequestURI();
        return ip + ":" + method + ":" + path;
    }
}

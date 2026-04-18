package com.linan.core.security.interceptor;

import com.linan.core.common.api.Result;
import com.linan.core.common.api.ResultCodeEnum;
import com.linan.core.security.JwtUtil;
import com.linan.core.security.PassAuth;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.stereotype.Component;

/**
 * JWT 鉴权拦截器
 *
 * 拦截所有请求，验证 Authorization Header 中的 JWT Token。
 * 被 @PassAuth 标记的接口会直接放行，不需要携带 Token。
 *
 * Token 验证失败时，直接向 Response 写入 JSON 错误信息并阻断请求，
 * 不会将异常抛给 GlobalExceptionHandler（因为拦截器在 Controller 之前执行）。
 */
@Slf4j
public class AuthInterceptor implements HandlerInterceptor {

    private final RedisTemplate<String, Object> redisTemplate;

    private final JwtUtil jwtUtil;

    /**
     * 构造函数注入 RedisTemplate 和 JwtUtil
     */
    public AuthInterceptor(RedisTemplate<String, Object> redisTemplate, JwtUtil jwtUtil) {
        this.redisTemplate = redisTemplate;
        this.jwtUtil = jwtUtil;
    }

    /**
     * ThreadLocal 存储当前请求的用户ID
     *
     * 为什么用 ThreadLocal？
     * 每个 HTTP 请求由独立的线程处理，ThreadLocal 可以安全地在线程内传递数据，
     * 无需在方法参数中层层传递 userId。Aspect 切面、Service 层都可直接获取。
     *
     * 使用 InheritableThreadLocal 确保子线程也能获取（如 @Async 异步任务）。
     */
    private static final ThreadLocal<Long> currentUserIdHolder = new InheritableThreadLocal<>();

    /**
     * 获取当前请求的用户ID（供 Aspect、Service 等任意位置调用）
     *
     * @return 当前登录用户的ID，未登录返回 null
     */
    public static Long getCurrentUserId() {
        return currentUserIdHolder.get();
    }

    /**
     * 清除 ThreadLocal（请求结束后必须调用，防止内存泄漏）
     */
    public static void clearCurrentUser() {
        currentUserIdHolder.remove();
    }

    /*
     * Jackson 序列化工具，用于将 Result 对象转为 JSON 字符串写入 Response。
     * ObjectMapper 是线程安全的，可以作为静态常量复用。
     */
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 请求前拦截
     *
     * 在请求到达 Controller 之前执行。返回 true 表示放行，false 表示拦截。
     *
     * 处理流程：
     * 1. 检查方法或类上是否有 @PassAuth 注解 -> 有则直接放行
     * 2. 从 Header 中提取 Authorization 字段 -> 为空则返回 401
     * 3. 解析 Token 并验证签名和有效期 -> 失败则返回 401
     * 4. 解析成功后，将 userId 和 username 存入 request 属性，供 Controller 使用
     *
     * @param request  当前 HTTP 请求
     * @param response 当前 HTTP 响应
     * @param handler  处理请求的 Controller 方法
     * @return true（放行）或 false（拦截并已写入错误响应）
     */
    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) throws Exception {

        // ---- 第一步：非 Controller 方法直接放行 ----
        // 只有实际处理请求的 Controller 方法才需要鉴权
        // 静态资源、OPTIONS 预检请求等不属于 HandlerMethod，直接放行
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;

        // ---- 第二步：检查 @PassAuth 注解 ----
        // 先检查方法级别的注解，再检查类级别的注解
        // 方法级别的注解优先级高于类级别
        if (handlerMethod.getMethodAnnotation(PassAuth.class) != null ||
            handlerMethod.getBeanType().getAnnotation(PassAuth.class) != null) {
            // @PassAuth 标记的接口，无论方法级别还是类级别，都直接放行
            return true;
        }

        // ---- 第三步：提取 Authorization Header ----
        // 标准格式：Authorization: Bearer <token>
        String authHeader = request.getHeader("Authorization");

        // 如果 Header 为空或格式不正确，返回 401 并中断请求
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeUnauthorizedResponse(response, "请求头中缺少Authorization信息");
            return false;
        }

        // 提取 Token 部分："Bearer " 后 7 个字符开始
        String token = authHeader.substring(7);

        // ---- 第四步：验证 Token ----
        try {
            // 1. 检查 Token 是否在黑名单中
            if (Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + token))) {
                writeUnauthorizedResponse(response, "Token已失效，请重新登录");
                return false;
            }

            // 2. 解析 Token，验证签名和有效期
            // 如果 Token 无效（过期、伪造、格式错误），会抛出对应的异常
            Claims claims = jwtUtil.parseToken(token);

            // 解析成功，存入 ThreadLocal（供全链路使用）和 request 属性
            Long userId = claims.get("userId", Long.class);
            currentUserIdHolder.set(userId);
            request.setAttribute("userId", userId);
            request.setAttribute("username", claims.get("username", String.class));

            return true; // 鉴权通过，放行

        } catch (ExpiredJwtException e) {
            log.warn("Token已过期: {}", e.getMessage());
            writeUnauthorizedResponse(response, "Token已过期，请重新登录");
            return false;

        } catch (SignatureException e) {
            log.warn("Token签名无效: {}", e.getMessage());
            writeUnauthorizedResponse(response, "Token签名无效");
            return false;

        } catch (MalformedJwtException e) {
            log.warn("Token格式错误: {}", e.getMessage());
            writeUnauthorizedResponse(response, "Token格式错误");
            return false;

        } catch (Exception e) {
            log.error("Token解析异常: ", e);
            writeUnauthorizedResponse(response, "Token无效，请重新登录");
            return false;
        } finally {
            // 请求处理完毕后，必须清除 ThreadLocal（防止内存泄漏）
            // 注意：此时不能清除，因为 Controller 和 Aspect 还未执行
            // 清除操作应在 afterCompletion 中进行
        }
    }

    /**
     * 请求处理完毕后清除 ThreadLocal
     *
     * Spring 会在视图渲染完成后调用此方法，
     * 此时 Controller 和 Aspect 都已执行完毕，可以安全清除。
     */
    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
                                 @NonNull HttpServletResponse response,
                                 @NonNull Object handler,
                                 @Nullable Exception ex) {
        clearCurrentUser();
    }

    /**
     * 向 Response 写入 401 未授权的 JSON 响应
     *
     * 由于拦截器在 GlobalExceptionHandler 之前执行，
     * 抛出的异常不会被全局异常处理器捕获，
     * 因此需要手动写入 JSON 响应到 Response 中。
     *
     * @param response HTTP 响应对象
     * @param message  错误提示信息
     */
    private void writeUnauthorizedResponse(HttpServletResponse response, String message) {
        try {
            // 设置响应内容类型为 JSON
            response.setContentType("application/json;charset=UTF-8");
            // 设置 HTTP 状态码为 200
            // （与框架统一返回体约定保持一致，通过 code 字段区分错误类型）
            response.setStatus(HttpServletResponse.SC_OK);

            // 构造错误返回体
            Result<Void> result = new Result<>(
                    ResultCodeEnum.UNAUTHORIZED.getCode(),
                    message != null ? message : ResultCodeEnum.UNAUTHORIZED.getMsg(),
                    null
            );

            // 将 Result 对象序列化为 JSON 字符串并写入响应
            String json = objectMapper.writeValueAsString(result);
            response.getWriter().write(json);
            response.getWriter().flush();

        } catch (Exception e) {
            // 写入响应失败，记录日志
            log.error("写入401响应失败: ", e);
        }
    }
}
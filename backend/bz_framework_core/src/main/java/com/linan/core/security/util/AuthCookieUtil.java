package com.linan.core.security.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 认证 Cookie（bz_token）工具
 *
 * <p>统一 httpOnly Cookie 的写入/清除逻辑，修复两处历史问题：</p>
 * <ul>
 *   <li>AuthController 旧实现 {@code addCookie + addHeader("Set-Cookie")} 会输出
 *       两条内容不一致的 Set-Cookie 头（浏览器可能采用任一，行为不确定）——
 *       此处改用 {@code response.setHeader("Set-Cookie", ...)} 单头输出；</li>
 *   <li>AuthInterceptor 滑动刷新只更新 X-Access-Token、不同步 Cookie，
 *       导致 Header/Cookie 双通道长期不一致——两处统一调用本工具。</li>
 * </ul>
 *
 * <p>Cookie 属性：HttpOnly（防 XSS 读取）+ Path=/ + Max-Age=86400（24h，与默认
 * JWT 过期时间对齐）+ HTTPS 请求带 Secure + SameSite 与 secure 联动
 * （secure → Strict，否则 → Lax），与既有行为保持一致。</p>
 */
public final class AuthCookieUtil {

    /** 认证 Cookie 名称 */
    public static final String COOKIE_NAME = "bz_token";

    /** Cookie 有效期（秒）：24 小时，与默认 JWT_EXPIRATION（86400000ms）对齐 */
    public static final int COOKIE_MAX_AGE = 86400;

    private AuthCookieUtil() {
    }

    /**
     * 设置 httpOnly 认证 Cookie（单头输出）
     *
     * <p>Secure 依据当前请求 scheme 判断：{@code https} 或代理头
     * {@code X-Forwarded-Proto: https} 视为安全通道（与 AuthController 旧实现一致）。</p>
     *
     * @param response HTTP 响应
     * @param token    JWT Token
     */
    public static void setAuthCookie(HttpServletResponse response, String token) {
        if (response == null || token == null) {
            return;
        }
        boolean secure = isSecureRequest();
        String sameSite = secure ? "SameSite=Strict" : "SameSite=Lax";
        // setHeader 单头输出：替换已有 Set-Cookie，避免 addCookie + addHeader 产生重复头
        response.setHeader("Set-Cookie",
                COOKIE_NAME + "=" + token + "; Path=/; Max-Age=" + COOKIE_MAX_AGE + "; "
                        + (secure ? "Secure; " : "") + "HttpOnly; " + sameSite);
    }

    /**
     * 清除 httpOnly 认证 Cookie（登出 / 改密时调用），Max-Age=0 立即过期
     */
    public static void clearAuthCookie(HttpServletResponse response) {
        if (response == null) {
            return;
        }
        response.setHeader("Set-Cookie",
                COOKIE_NAME + "=; Path=/; Max-Age=0; HttpOnly; SameSite=Strict");
    }

    /**
     * 判断当前请求是否走 HTTPS（真实 scheme 或可信代理头 X-Forwarded-Proto）
     */
    private static boolean isSecureRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null || attributes.getRequest() == null) {
            return false;
        }
        HttpServletRequest request = attributes.getRequest();
        return "https".equalsIgnoreCase(request.getScheme())
                || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
    }
}
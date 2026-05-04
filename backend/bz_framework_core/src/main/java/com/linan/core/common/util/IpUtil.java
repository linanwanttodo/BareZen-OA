package com.linan.core.common.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 客户端真实 IP 获取工具
 *
 * <p>统一收敛限流、防重、操作日志、登录日志等场景的 IP 提取逻辑，
 * 并通过 {@code security.trust-proxy-headers} 配置开关控制是否信任代理头，
 * 解决「直接信任 X-Forwarded-For 可被伪造绕过限流/伪造他人 IP 误伤」的问题。</p>
 *
 * <h3>行为说明</h3>
 * <ul>
 *   <li>{@code security.trust-proxy-headers=false}（默认）：只取 {@code getRemoteAddr()}，
 *       适用于后端直连客户端或前置代理未配置可信重写头的情况，无法被伪造。</li>
 *   <li>{@code security.trust-proxy-headers=true}：依次信任
 *       X-Forwarded-For（取第一个 IP）→ X-Real-IP → RemoteAddr。
 *       仅当部署了<b>可控的</b>反向代理（Nginx 等）并已配置覆盖这两个头时开启，
 *       否则客户端可伪造该头绕过限流。</li>
 * </ul>
 *
 * @author BareZen Team
 */
@Slf4j
@Component
public class IpUtil {

    /** 是否信任代理头（X-Forwarded-For / X-Real-IP），默认 false 防伪造 */
    private final boolean trustProxyHeaders;

    public IpUtil(@Value("${security.trust-proxy-headers:false}") boolean trustProxyHeaders) {
        this.trustProxyHeaders = trustProxyHeaders;
    }

    /**
     * 获取客户端真实 IP
     *
     * @param request HTTP 请求
     * @return 客户端 IP，获取失败返回 "unknown"
     */
    public String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String ip;
        if (trustProxyHeaders) {
            // 信任代理头：X-Forwarded-For（取第一个 IP，最接近客户端）→ X-Real-IP
            ip = request.getHeader("X-Forwarded-For");
            if (isEmptyOrUnknown(ip)) {
                ip = request.getHeader("X-Real-IP");
            }
            if (isEmptyOrUnknown(ip)) {
                ip = request.getRemoteAddr();
            }
            if (ip != null && ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
        } else {
            // 默认：仅取 socket 地址，忽略可伪造的代理头
            ip = request.getRemoteAddr();
        }
        return ip != null && !ip.isEmpty() ? ip : "unknown";
    }

    private boolean isEmptyOrUnknown(String ip) {
        return ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip);
    }
}

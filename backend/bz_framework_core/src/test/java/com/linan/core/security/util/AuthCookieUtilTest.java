package com.linan.core.security.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AuthCookieUtil 单元测试
 *
 * <p>覆盖：仅输出一条 Set-Cookie 头且属性完整 / HTTPS 请求带 Secure + SameSite=Strict /
 * clear 立即过期（Max-Age=0）。</p>
 */
class AuthCookieUtilTest {

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void setAuthCookie_singleHeaderWithFullAttributes() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/v1/login");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        MockHttpServletResponse response = new MockHttpServletResponse();

        AuthCookieUtil.setAuthCookie(response, "jwt-token-abc");

        // 必须仅输出一条 Set-Cookie 头（修复 addCookie + addHeader 产生的重复头）
        assertEquals(1, response.getHeaders("Set-Cookie").size(), "必须仅输出一条 Set-Cookie 头");
        String header = response.getHeader("Set-Cookie");
        assertNotNull(header);
        assertTrue(header.startsWith("bz_token=jwt-token-abc;"), "cookie 名与值: " + header);
        assertTrue(header.contains("Path=/"), "必须含 Path=/: " + header);
        assertTrue(header.contains("Max-Age=86400"), "必须含 Max-Age=86400: " + header);
        assertTrue(header.contains("HttpOnly"), "必须含 HttpOnly: " + header);
        assertTrue(header.contains("SameSite=Lax"), "HTTP 请求下 SameSite=Lax: " + header);
        assertFalse(header.contains("Secure"), "HTTP 请求下不得输出 Secure: " + header);
    }

    @Test
    void setAuthCookie_httpsAddsSecureAndStrict() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/v1/login");
        request.setScheme("https");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        MockHttpServletResponse response = new MockHttpServletResponse();

        AuthCookieUtil.setAuthCookie(response, "jwt-token-abc");

        assertEquals(1, response.getHeaders("Set-Cookie").size());
        String header = response.getHeader("Set-Cookie");
        assertNotNull(header);
        assertTrue(header.contains("Secure"), "HTTPS 请求下必须带 Secure: " + header);
        assertTrue(header.contains("SameSite=Strict"), "HTTPS 请求下 SameSite=Strict: " + header);
    }

    @Test
    void setAuthCookie_proxyForwardedProtoHttpsAddsSecure() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/v1/login");
        request.setScheme("http");
        request.addHeader("X-Forwarded-Proto", "https");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        MockHttpServletResponse response = new MockHttpServletResponse();

        AuthCookieUtil.setAuthCookie(response, "jwt-token-abc");

        String header = response.getHeader("Set-Cookie");
        assertNotNull(header);
        assertTrue(header.contains("Secure"), "X-Forwarded-Proto=https 时必须带 Secure: " + header);
    }

    @Test
    void clearAuthCookie_expiresImmediately() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        AuthCookieUtil.clearAuthCookie(response);

        assertEquals(1, response.getHeaders("Set-Cookie").size());
        String header = response.getHeader("Set-Cookie");
        assertNotNull(header);
        assertTrue(header.startsWith("bz_token=;"), "清除时值为空: " + header);
        assertTrue(header.contains("Max-Age=0"), "清除必须 Max-Age=0: " + header);
        assertTrue(header.contains("HttpOnly"), "清除必须含 HttpOnly: " + header);
    }
}
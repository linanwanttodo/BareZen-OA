package com.linan.core.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AuthNoStoreFilter 单元测试
 *
 * <p>直接调用 doFilter（MockHttpServletRequest/Response），验证：
 * 认证路径带头、开放路径带头、业务路径不带头、过滤器放行后续链路。</p>
 */
class AuthNoStoreFilterTest {

    private final AuthNoStoreFilter filter = new AuthNoStoreFilter();

    @Test
    void authPath_setsNoStoreHeaders() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/v1/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals("no-store", response.getHeader("Cache-Control"));
        assertEquals("no-cache", response.getHeader("Pragma"));
    }

    @Test
    void openPath_setsNoStoreHeaders() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/open/v1/attendance");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals("no-store", response.getHeader("Cache-Control"));
        assertEquals("no-cache", response.getHeader("Pragma"));
    }

    @Test
    void businessPath_noNoStoreHeaders() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/system/v1/users");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertNull(response.getHeader("Cache-Control"));
        assertNull(response.getHeader("Pragma"));
    }

    @Test
    void filterChainStillProcessed() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/v1/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNotNull(chain.getRequest(), "过滤器应放行后续链路");
        assertNotNull(chain.getResponse());
    }
}
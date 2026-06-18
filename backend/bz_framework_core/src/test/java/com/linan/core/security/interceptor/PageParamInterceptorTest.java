package com.linan.core.security.interceptor;

import com.linan.core.common.api.PageParamGuard;
import com.linan.core.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * PageParamInterceptor 兜底放行与拒绝单测
 *
 * <p>该拦截器是 23 个未主动校验的存量接口的唯一防线，且阈值取自 SQL 层硬上限，
 * 需要同时锁定"不越权拦截「拉全量」调用"与"拦住真正无界的请求"两侧。</p>
 */
@ExtendWith(MockitoExtension.class)
class PageParamInterceptorTest {

    @Mock
    private HttpServletRequest request;

    private final PageParamInterceptor interceptor = new PageParamInterceptor();

    private void stubParams(String pageNum, String pageSize) {
        lenient().when(request.getMethod()).thenReturn("GET");
        lenient().when(request.getParameter("pageNum")).thenReturn(pageNum);
        lenient().when(request.getParameter("pageSize")).thenReturn(pageSize);
    }

    @Test
    @DisplayName("未携带分页参数时直接放行")
    void preHandle_noPageParams_passes() {
        stubParams(null, null);
        assertTrue(interceptor.preHandle(request, null, null));
    }

    @Test
    @DisplayName("GET 携带硬上限内的 pageSize 放行（存量「拉全量」调用不受影响）")
    void preHandle_withinHardCap_passes() {
        stubParams("1", String.valueOf(PageParamGuard.ABSOLUTE_MAX_PAGE_SIZE));
        assertTrue(interceptor.preHandle(request, null, null));
    }

    @Test
    @DisplayName("超过 SQL 层硬上限时拒绝，不再被静默截断")
    void preHandle_aboveHardCap_rejected() {
        stubParams("1", String.valueOf(PageParamGuard.ABSOLUTE_MAX_PAGE_SIZE + 1));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> interceptor.preHandle(request, null, null));
        assertEquals(400, ex.getCode());
        assertEquals(PageParamGuard.INVALID_MESSAGE, ex.getMessage());
    }

    @Test
    @DisplayName("仅传 pageNum 时不因缺少 pageSize 而误判")
    void preHandle_onlyPageNum_passes() {
        stubParams("3", null);
        assertTrue(interceptor.preHandle(request, null, null));
    }

    @Test
    @DisplayName("非数字分页参数按非法拒绝")
    void preHandle_nonNumeric_rejected() {
        stubParams("1", "abc");
        assertThrows(BusinessException.class, () -> interceptor.preHandle(request, null, null));
    }

    @Test
    @DisplayName("CORS 预检请求跳过校验，不产生业务错误")
    void preHandle_optionsPassesThrough() {
        when(request.getMethod()).thenReturn("OPTIONS");
        assertTrue(interceptor.preHandle(request, null, null));
    }
}

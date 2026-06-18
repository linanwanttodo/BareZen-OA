package com.linan.core.security.interceptor;

import com.linan.core.common.api.PageParamGuard;
import com.linan.core.common.exception.BusinessException;
import com.linan.core.common.api.ResultCodeEnum;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 分页参数兜底拦截器
 *
 * <p>覆盖未主动调用 {@link PageParamGuard#validate} 的存量接口，防止分页参数越界后
 * 依赖 SQL 层 {@code maxLimit} 静默截断。阈值取硬上限而非业务上限 100，
 * 以免打断"一次拉全量"的存量调用（如角色下拉框）。</p>
 *
 * <p>只作用于 URL 查询参数；以 JSON 体传分页参数的接口仍需 Controller 自行校验。</p>
 */
public class PageParamInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // CORS 预检不带业务参数，且此时不应产生 4xx 业务响应
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        Integer pageNum = parse(request.getParameter("pageNum"));
        Integer pageSize = parse(request.getParameter("pageSize"));
        PageParamGuard.validateSuppliedWithinHardCap(pageNum, pageSize);
        return true;
    }

    /** 非数字按非法处理，避免落到 Controller 后产生与校验规则不一致的类型转换错误 */
    private Integer parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(raw.trim());
        } catch (NumberFormatException e) {
            throw new BusinessException(ResultCodeEnum.VALIDATE_FAILED, PageParamGuard.INVALID_MESSAGE);
        }
    }
}

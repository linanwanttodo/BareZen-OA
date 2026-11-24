package com.linan.core.security.interceptor;

import com.linan.core.common.api.Result;
import com.linan.core.common.api.ResultCodeEnum;
import com.linan.core.security.JwtUtil;
import com.linan.core.spi.permission.ApiKeyValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 开放平台（Open API）鉴权拦截器
 *
 * <p>拦截 {@code /api/open/**} 路径，通过 {@code X-Api-Key} 请求头认证外部系统
 * （对应市场调研中「系统集成」需求）。API Key 由管理端生成、库中存 SHA-256 哈希，
 * 校验通过后将 scopes 写入 request 属性，供 {@code @OpenScope} 注解按范围放行。</p>
 *
 * <p>与 {@link AuthInterceptor} 的关系：两者互斥注册——/api/open/** 走本拦截器，
 * 其余 /api/** 走 JWT 拦截器，避免开放接口被用户 Token 通道误拦。</p>
 *
 * @author BareZen Team
 */
@Slf4j
public class OpenApiInterceptor implements HandlerInterceptor {

    /** 请求头：外部系统携带的明文 API Key */
    public static final String HEADER_API_KEY = "X-" + "Api" + "-Key";

    /** request 属性：校验通过的 API Key scopes */
    public static final String ATTR_SCOPES = "openApiScopes";

    private final ApiKeyValidator apiKeyValidator;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public OpenApiInterceptor(ApiKeyValidator apiKeyValidator) {
        this.apiKeyValidator = apiKeyValidator;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        String apiKey = request.getHeader(HEADER_API_KEY);
        String scopes = apiKeyValidator.scopes(apiKey);
        if (scopes == null) {
            writeUnauthorizedResponse(response, "API Key 无效、已禁用或已过期");
            return false;
        }

        request.setAttribute(ATTR_SCOPES, scopes);
        return true;
    }

    private void writeUnauthorizedResponse(HttpServletResponse response, String message) {
        try {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            Result<Void> result = new Result<>(
                    ResultCodeEnum.UNAUTHORIZED.getCode(),
                    message,
                    null
            );
            response.getWriter().write(objectMapper.writeValueAsString(result));
            response.getWriter().flush();
        } catch (Exception e) {
            log.error("写入 Open API 401 响应失败: ", e);
        }
    }
}

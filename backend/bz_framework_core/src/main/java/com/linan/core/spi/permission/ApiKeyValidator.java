package com.linan.core.spi.permission;

/**
 * 开放平台 API Key 校验 SPI（core → system 依赖反转）
 *
 * <p>core 的 {@link com.linan.core.security.interceptor.OpenApiInterceptor} 只依赖本契约，
 * 具体的哈希匹配/过期判定由 com.linan.system 侧实现。</p>
 *
 * @author BareZen Team
 */
public interface ApiKeyValidator {

    /**
     * 校验明文 API Key
     *
     * @param apiKey 请求头 {@code X-Api-Key} 携带的明文密钥
     * @return 有效时返回其权限范围 scopes（可能为空串）；无效/禁用/过期返回 {@code null}
     */
    String scopes(String apiKey);
}
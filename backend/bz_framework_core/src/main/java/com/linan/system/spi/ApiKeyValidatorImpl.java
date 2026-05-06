package com.linan.system.spi;

import com.linan.core.spi.permission.ApiKeyValidator;
import com.linan.system.entity.SysApiKey;
import com.linan.system.service.ISysApiKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 开放平台 API Key 校验 SPI 的系统侧实现
 *
 * <p>委托 {@link ISysApiKeyService#validateApiKey} 完成哈希匹配/启用/过期判定，
 * 使 core 的 Open API 拦截器不依赖系统实体。</p>
 *
 * @author BareZen Team
 */
@Component
@RequiredArgsConstructor
public class ApiKeyValidatorImpl implements ApiKeyValidator {

    private final ISysApiKeyService apiKeyService;

    @Override
    public String scopes(String apiKey) {
        SysApiKey key = apiKeyService.validateApiKey(apiKey);
        return key == null ? null : key.getScopes();
    }
}
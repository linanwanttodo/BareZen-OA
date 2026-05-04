package com.linan.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.linan.system.entity.SysApiKey;

/**
 * 开放平台 API Key 服务接口
 *
 * @author BareZen Team
 */
public interface ISysApiKeyService extends IService<SysApiKey> {

    /**
     * 校验 API Key 是否有效（启用 + 未过期 + 哈希匹配）
     *
     * @param apiKey 外部系统传入的明文 API Key（X-Api-Key 请求头）
     * @return 有效返回密钥实体，无效返回 null
     */
    SysApiKey validateApiKey(String apiKey);

    /**
     * 校验 API Key 是否具备指定权限范围
     *
     * @param apiKey 明文 API Key
     * @param scope  所需权限范围（如 open:approval:read）
     * @return 有效且包含该范围返回 true
     */
    boolean hasScope(String apiKey, String scope);
}

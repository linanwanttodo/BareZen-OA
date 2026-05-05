package com.linan.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.linan.system.entity.SysApiKey;

import java.time.LocalDateTime;

/**
 * 开放平台 API Key 服务接口
 *
 * @author BareZen Team
 */
public interface ISysApiKeyService extends IService<SysApiKey> {

    /**
     * 创建结果：明文 Key 仅在创建响应中返回一次
     *
     * @param id         密钥记录主键
     * @param apiKey     明文 API Key（仅此一次返回，库中只存哈希）
     * @param keyName    密钥名称
     * @param scopes     权限范围
     * @param status     状态：0=启用
     * @param expireTime 过期时间
     */
    record SysApiKeyCreated(Long id, String apiKey, String keyName, String scopes,
                            String status, LocalDateTime expireTime) {
    }

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

    /**
     * 创建 API Key（管理端）
     *
     * <p>生成 {@code bz_xxx} 格式随机密钥，库中仅存 SHA-256 哈希，
     * 明文只通过返回值出现一次，后续任何接口不再返回明文。</p>
     *
     * @param keyName    密钥名称（用途标识，必填）
     * @param scopes     权限范围，逗号分隔，可为空
     * @param expireTime 过期时间，可为空（空=永不过期）
     * @return 创建结果（含一次性的明文 Key）
     */
    SysApiKeyCreated createApiKey(String keyName, String scopes, LocalDateTime expireTime);
}

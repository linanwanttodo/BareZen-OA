package com.linan.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.system.entity.SysApiKey;
import com.linan.system.mapper.SysApiKeyMapper;
import com.linan.system.service.ISysApiKeyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 开放平台 API Key 服务实现
 *
 * <p>API Key 采用「明文只在生成时返回一次，库中仅存 SHA-256 哈希」策略
 * （与密码存储一致），校验时对传入 Key 哈希后比对，防止数据库泄露导致 Key 可逆。</p>
 *
 * @author BareZen Team
 */
@Slf4j
@Service
public class SysApiKeyServiceImpl extends ServiceImpl<SysApiKeyMapper, SysApiKey>
        implements ISysApiKeyService {

    @Override
    public SysApiKey validateApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        String hash = sha256(apiKey);
        SysApiKey key = getOne(new LambdaQueryWrapper<SysApiKey>()
                .eq(SysApiKey::getApiKeyHash, hash)
                .eq(SysApiKey::getStatus, "0")      // 仅启用
                .last("LIMIT 1"));
        if (key == null) {
            return null;
        }
        // 过期校验（expire_time 为空 = 永不过期）
        if (key.getExpireTime() != null && key.getExpireTime().isBefore(LocalDateTime.now())) {
            log.warn("API Key 已过期: {}", key.getKeyName());
            return null;
        }
        return key;
    }

    @Override
    public boolean hasScope(String apiKey, String scope) {
        SysApiKey key = validateApiKey(apiKey);
        if (key == null || key.getScopes() == null || key.getScopes().isBlank()) {
            return false;
        }
        Set<String> scopes = Arrays.stream(key.getScopes().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        return scopes.contains(scope) || scopes.contains("*");
    }

    @Override
    public SysApiKeyCreated createApiKey(String keyName, String scopes, LocalDateTime expireTime) {
        // 明文 Key：固定前缀 + 32 位随机十六进制（足够熵，无连字符便于复制）
        String plain = "bz_" + UUID.randomUUID().toString().replace("-", "");
        SysApiKey key = new SysApiKey();
        key.setKeyName(keyName);
        key.setApiKeyHash(sha256(plain));
        key.setScopes(scopes);
        key.setStatus("0");
        key.setExpireTime(expireTime);
        save(key);
        return new SysApiKeyCreated(key.getId(), plain, keyName, scopes, "0", expireTime);
    }

    /**
     * SHA-256 哈希（十六进制小写，统一走 DigestUtil）
     */
    private String sha256(String input) {
        return com.linan.core.common.util.DigestUtil.sha256Hex(input);
    }
}

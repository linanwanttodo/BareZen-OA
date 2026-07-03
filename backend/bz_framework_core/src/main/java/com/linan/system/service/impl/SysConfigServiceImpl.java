package com.linan.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.system.entity.SysConfig;
import com.linan.system.mapper.SysConfigMapper;
import com.linan.system.service.ISysConfigService;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * 系统配置 Service 实现
 *
 * <p>配置值缓存 5 分钟，管理员修改配置时自动清除缓存。</p>
 *
 * @author BareZen Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig> implements ISysConfigService {

    /** 配置缓存名：注解与手工逐出共用同一常量，避免两处字面量漂移 */
    private static final String CACHE_NAME = "sys_config";

    private final CacheManager cacheManager;

    @Override
    @Cacheable(value = CACHE_NAME, key = "#configKey", unless = "#result == null")
    public String selectConfigByKey(String configKey) {
        return baseMapper.selectConfigValueByKey(configKey);
    }

    @Override
    @CacheEvict(value = CACHE_NAME, key = "#config.configKey")
    public boolean updateConfig(SysConfig config) {
        // 先取旧键名：管理员可能顺手改了 key，只逐出新键会让旧键缓存残留
        SysConfig before = config.getId() == null ? null : baseMapper.selectById(config.getId());
        boolean updated = this.updateById(config);
        if (updated && before != null && !Objects.equals(before.getConfigKey(), config.getConfigKey())) {
            // 必须经 CacheManager 逐出：调用本类 clearCache() 属自调用、不过代理，@CacheEvict 不生效
            Cache cache = cacheManager.getCache(CACHE_NAME);
            if (cache != null) {
                cache.evict(before.getConfigKey());
            }
            log.debug("配置键名变更，已同时清除旧键缓存: {} -> {}", before.getConfigKey(), config.getConfigKey());
        }
        if (updated) {
            log.debug("配置已更新并清除缓存: {}", config.getConfigKey());
        }
        return updated;
    }

    @Override
    @CacheEvict(value = CACHE_NAME, key = "#configKey")
    public void clearCache(String configKey) {
        log.debug("已清除配置缓存: {}", configKey);
    }

    @Override
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void clearAllCache() {
        log.debug("已清除全部配置缓存");
    }
}

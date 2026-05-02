package com.linan.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.system.entity.SysConfig;
import com.linan.system.mapper.SysConfigMapper;
import com.linan.system.service.ISysConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * 系统配置 Service 实现
 *
 * <p>配置值缓存 5 分钟，管理员修改配置时自动清除缓存。</p>
 *
 * @author BareZen Team
 */
@Slf4j
@Service
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig> implements ISysConfigService {

    @Override
    @Cacheable(value = "sys_config", key = "#configKey", unless = "#result == null")
    public String selectConfigByKey(String configKey) {
        return baseMapper.selectConfigValueByKey(configKey);
    }

    @Override
    @CacheEvict(value = "sys_config", key = "#config.configKey")
    public boolean updateConfig(SysConfig config) {
        boolean updated = this.updateById(config);
        if (updated) {
            log.debug("配置已更新并清除缓存: {}", config.getConfigKey());
        }
        return updated;
    }

    @Override
    @CacheEvict(value = "sys_config", key = "#configKey")
    public void clearCache(String configKey) {
        log.debug("已清除配置缓存: {}", configKey);
    }

    @Override
    @CacheEvict(value = "sys_config", allEntries = true)
    public void clearAllCache() {
        log.debug("已清除全部配置缓存");
    }
}

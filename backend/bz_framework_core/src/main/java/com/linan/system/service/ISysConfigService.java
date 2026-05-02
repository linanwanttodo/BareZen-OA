package com.linan.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.linan.system.entity.SysConfig;

/**
 * 系统配置 Service 接口
 *
 * @author BareZen Team
 */
public interface ISysConfigService extends IService<SysConfig> {

    /**
     * 根据配置键名查询配置值（带 Redis 缓存）
     */
    String selectConfigByKey(String configKey);

    /**
     * 更新配置值（同时清除缓存）
     */
    boolean updateConfig(SysConfig config);

    /**
     * 清除指定配置的缓存
     */
    void clearCache(String configKey);

    /**
     * 清除全部配置缓存
     */
    void clearAllCache();
}

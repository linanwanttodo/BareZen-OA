package com.linan.system.service.impl;

import com.linan.system.entity.SysConfig;
import com.linan.system.mapper.SysConfigMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 配置缓存逐出行为测试。
 *
 * <p>@Cacheable 按 configKey 缓存且 TTL 5 分钟，因此「改了键名但只逐出新键」会让
 * 旧键在 TTL 内继续吐出改名前的值——业务读到的仍是旧配置，属于静默的数据不一致。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("系统配置Service测试")
class SysConfigServiceImplTest {

    @Mock
    private SysConfigMapper configMapper;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    private SysConfigServiceImpl configService;

    @BeforeEach
    void setUp() {
        // ServiceImpl 的 baseMapper 为 protected 泛型字段，@InjectMocks 注不进去
        configService = new SysConfigServiceImpl(cacheManager);
        ReflectionTestUtils.setField(configService, "baseMapper", configMapper);
        lenient().when(cacheManager.getCache("sys_config")).thenReturn(cache);
    }

    private SysConfig config(Long id, String key, String value) {
        SysConfig c = new SysConfig();
        c.setId(id);
        c.setConfigKey(key);
        c.setConfigValue(value);
        return c;
    }

    @Test
    @DisplayName("修改配置键名时同时逐出旧键缓存，避免旧键读到改名前的值")
    void updateConfig_keyRenamed_alsoEvictsOldKey() {
        when(configMapper.selectById(9L)).thenReturn(config(9L, "old.key", "v1"));
        when(configMapper.updateById(any(SysConfig.class))).thenReturn(1);

        assertTrue(configService.updateConfig(config(9L, "new.key", "v2")));

        verify(cache).evict("old.key");
    }

    @Test
    @DisplayName("未改键名时不做多余的旧键逐出")
    void updateConfig_keyUnchanged_noExtraEviction() {
        when(configMapper.selectById(9L)).thenReturn(config(9L, "keep.key", "v1"));
        when(configMapper.updateById(any(SysConfig.class))).thenReturn(1);

        assertTrue(configService.updateConfig(config(9L, "keep.key", "v2")));

        verify(cache, never()).evict(anyString());
    }
}

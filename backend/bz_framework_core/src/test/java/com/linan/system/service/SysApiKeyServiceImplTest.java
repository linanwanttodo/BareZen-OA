package com.linan.system.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.linan.system.entity.SysApiKey;
import com.linan.system.mapper.SysApiKeyMapper;
import com.linan.system.service.impl.SysApiKeyServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SysApiKeyServiceImpl 单元测试
 *
 * <p>覆盖：createApiKey 生成/存哈希/明文仅一次；validateApiKey 启用+未过期校验；
 * hasScope 范围匹配（含通配符 *）。</p>
 */
@ExtendWith(MockitoExtension.class)
class SysApiKeyServiceImplTest {

    private static final Pattern HEX64 = Pattern.compile("^[0-9a-f]{64}$");

    @Mock
    private SysApiKeyMapper apiKeyMapper;

    private SysApiKeyServiceImpl apiKeyService;

    @BeforeAll
    static void initTableInfoCache() {
        // 初始化实体列映射，使 getOne 内部的 LambdaQueryWrapper 在纯 Mockito 环境下可用
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), SysApiKey.class);
    }

    @BeforeEach
    void setUp() {
        apiKeyService = new SysApiKeyServiceImpl();
        // baseMapper（继承自 MyBatis-Plus ServiceImpl）反射注入
        ReflectionTestUtils.setField(apiKeyService, "baseMapper", apiKeyMapper);
    }

    @Nested
    @DisplayName("createApiKey()")
    class CreateApiKey {

        @Test
        @DisplayName("生成 bz_ 前缀明文密钥，库中仅存 SHA-256 哈希，状态启用")
        void createsKey_storesHashOnly() {
            // mock insert 不自动回填主键，模拟 MyBatis-Plus 行为回填 id
            doAnswer(inv -> {
                inv.<SysApiKey>getArgument(0).setId(9L);
                return 1;
            }).when(apiKeyMapper).insert(any(SysApiKey.class));

            var created = apiKeyService.createApiKey("测试密钥", "open:approval:read", null);

            assertNotNull(created.id());
            assertTrue(created.apiKey().startsWith("bz_"), "明文 Key 必须带 bz_ 前缀");
            assertEquals("测试密钥", created.keyName());
            assertEquals("open:approval:read", created.scopes());
            assertEquals("0", created.status());

            // 库中存的是哈希，不是明文
            ArgumentCaptor<SysApiKey> captor = ArgumentCaptor.forClass(SysApiKey.class);
            verify(apiKeyMapper).insert(captor.capture());
            SysApiKey saved = captor.getValue();
            assertTrue(HEX64.matcher(saved.getApiKeyHash()).matches(), "哈希必须是 64 位十六进制");
            assertNotEquals(created.apiKey(), saved.getApiKeyHash(), "明文绝不落库");
            assertEquals("0", saved.getStatus());
        }

        @Test
        @DisplayName("传入过期时间原样落库，空为永不过期")
        void expireTime_passedThrough() {
            LocalDateTime expire = LocalDateTime.of(2026, 12, 31, 23, 59, 59);

            var created = apiKeyService.createApiKey("短期密钥", null, expire);

            assertEquals(expire, created.expireTime());
            ArgumentCaptor<SysApiKey> captor = ArgumentCaptor.forClass(SysApiKey.class);
            verify(apiKeyMapper).insert(captor.capture());
            assertEquals(expire, captor.getValue().getExpireTime());
        }
    }

    @Nested
    @DisplayName("validateApiKey()")
    class ValidateApiKey {

        @Test
        @DisplayName("null / 空白 返回 null，不触库")
        void blankKey_returnsNull() {
            assertNull(apiKeyService.validateApiKey(null));
            assertNull(apiKeyService.validateApiKey("   "));
            verify(apiKeyMapper, never()).selectOne(any());
        }

        @Test
        @DisplayName("启用且未过期返回密钥实体")
        void enabledAndNotExpired_returnsKey() {
            SysApiKey key = new SysApiKey();
            key.setId(1L);
            key.setApiKeyHash("a".repeat(64));
            key.setStatus("0");
            key.setExpireTime(LocalDateTime.now().plusDays(30));
            // MyBatis-Plus getOne 实际调用 selectOne(wrapper, true) 双参数重载
            when(apiKeyMapper.selectOne(any(), anyBoolean())).thenReturn(key);

            SysApiKey result = apiKeyService.validateApiKey("bz_whatever");

            assertNotNull(result);
            assertEquals(1L, result.getId());
        }

        @Test
        @DisplayName("已禁用返回 null（查询条件排除禁用状态）")
        void disabled_returnsNull() {
            // 真实实现：查询 wrapper 带 status='0' 条件，禁用 key 查不到 → 返回 null
            when(apiKeyMapper.selectOne(any(), anyBoolean())).thenReturn(null);

            assertNull(apiKeyService.validateApiKey("bz_whatever"));

            // 验证查询条件确实带启用状态过滤，而非依赖业务判断
            ArgumentCaptor<LambdaQueryWrapper<SysApiKey>> captor =
                    ArgumentCaptor.forClass(LambdaQueryWrapper.class);
            verify(apiKeyMapper).selectOne(captor.capture(), anyBoolean());
            assertTrue(captor.getValue().getSqlSegment().contains("status"),
                    "查询必须按启用状态过滤");
        }

        @Test
        @DisplayName("已过期返回 null")
        void expired_returnsNull() {
            SysApiKey key = new SysApiKey();
            key.setStatus("0");
            key.setExpireTime(LocalDateTime.now().minusDays(1));
            when(apiKeyMapper.selectOne(any(), anyBoolean())).thenReturn(key);

            assertNull(apiKeyService.validateApiKey("bz_whatever"));
        }
    }

    @Nested
    @DisplayName("hasScope()")
    class HasScope {

        private SysApiKey keyWithScopes(String scopes) {
            SysApiKey key = new SysApiKey();
            key.setStatus("0");
            key.setScopes(scopes);
            when(apiKeyMapper.selectOne(any(), anyBoolean())).thenReturn(key);
            return key;
        }

        @Test
        @DisplayName("范围精确匹配返回 true")
        void exactScope_returnsTrue() {
            keyWithScopes("open:approval:read,open:supply:read");
            assertTrue(apiKeyService.hasScope("bz_x", "open:approval:read"));
        }

        @Test
        @DisplayName("通配符 * 匹配任意范围")
        void wildcard_returnsTrue() {
            keyWithScopes("*");
            assertTrue(apiKeyService.hasScope("bz_x", "open:anything"));
        }

        @Test
        @DisplayName("范围不匹配 / 空范围返回 false")
        void mismatched_returnsFalse() {
            keyWithScopes("open:approval:read");
            assertFalse(apiKeyService.hasScope("bz_x", "open:supply:read"));

            keyWithScopes("");
            assertFalse(apiKeyService.hasScope("bz_x", "open:approval:read"));
        }

        @Test
        @DisplayName("Key 无效（未启用查不到）返回 false")
        void invalidKey_returnsFalse() {
            // 查询条件带 status='0'，禁用 key 查不到 → validateApiKey 返回 null
            when(apiKeyMapper.selectOne(any(), anyBoolean())).thenReturn(null);

            assertFalse(apiKeyService.hasScope("bz_x", "open:approval:read"));
        }
    }
}
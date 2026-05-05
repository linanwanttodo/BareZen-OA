package com.linan.system.controller;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.security.RequirePermission;
import com.linan.system.entity.SysApiKey;
import com.linan.system.service.ISysApiKeyService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * SysApiKeyController 单元测试
 *
 * <p>覆盖：分页脱敏展示（绝不返回哈希/明文）、创建仅一次返回明文、
 * 参数校验、@RequirePermission 权限串。secret 存储策略见 SysApiKeyServiceImpl。</p>
 */
@ExtendWith(MockitoExtension.class)
class SysApiKeyControllerTest {

    @Mock
    private ISysApiKeyService apiKeyService;

    private SysApiKeyController controller;

    @BeforeAll
    static void initTableInfoCache() {
        // controller 内构建 LambdaQueryWrapper 依赖实体列映射
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), SysApiKey.class);
    }

    private SysApiKeyController newController() {
        return new SysApiKeyController(apiKeyService);
    }

    private SysApiKey sampleKey() {
        SysApiKey key = new SysApiKey();
        key.setId(1L);
        key.setKeyName("对接系统");
        key.setApiKeyHash("abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789");
        key.setScopes("open:approval:read");
        key.setStatus("0");
        key.setExpireTime(null);
        key.setCreateTime(LocalDateTime.of(2026, 8, 5, 10, 0));
        return key;
    }

    @Test
    @DisplayName("分页返回脱敏 maskedKey，绝不返回哈希与明文")
    void page_masksKeyHash() {
        Page<SysApiKey> page = new Page<>(1, 10);
        page.setRecords(List.of(sampleKey()));
        page.setTotal(1);
        when(apiKeyService.page(any(), any())).thenReturn(page);

        controller = newController();
        Result<?> result = controller.page(1, 10, null);

        assertEquals(200, result.getCode());
        PageResult<Map<String, Object>> pr = (PageResult<Map<String, Object>>) result.getData();
        Map<String, Object> item = pr.getList().get(0);
        String masked = (String) item.get("maskedKey");
        assertTrue(masked.startsWith("bz_****"), "脱敏格式：bz_**** + 末4位");
        assertTrue(masked.endsWith("6789"), "哈希末4位用于区分标识");
        assertFalse(item.containsKey("apiKeyHash"), "绝不返回哈希原文");
        assertFalse(item.containsKey("apiKey"), "绝不返回明文");
    }

    @Test
    @DisplayName("分页参数非法返回业务错误")
    void page_invalidParams_returnsError() {
        controller = newController();
        assertEquals(500, controller.page(0, 10, null).getCode());
        assertEquals(500, controller.page(1, 101, null).getCode());
    }

    @Test
    @DisplayName("创建成功明文 Key 仅本次返回一次")
    void create_returnsPlainOnce() {
        ISysApiKeyService.SysApiKeyCreated created =
                new ISysApiKeyService.SysApiKeyCreated(9L, "bz_secretplain", "对接系统",
                        "open:approval:read", "0", null);
        when(apiKeyService.createApiKey("对接系统", null, null)).thenReturn(created);

        controller = newController();
        Result<?> result = controller.create(Map.of("keyName", "对接系统"));

        assertEquals(200, result.getCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertEquals("bz_secretplain", data.get("apiKey"), "明文仅在创建响应返回一次");
    }

    @Test
    @DisplayName("创建时密钥名称为空返回业务错误")
    void create_blankName_returnsError() {
        controller = newController();
        assertEquals(500, controller.create(Map.of("keyName", "  ")).getCode());
        assertEquals(500, controller.create(Map.of()).getCode());
    }

    @Test
    @DisplayName("启停/删除接口声明权限串与 F 菜单一致")
    void permissions_alignedWithMenu() throws Exception {
        assertEquals("system:apiKey:list",
                SysApiKeyController.class.getMethod("page", Integer.class, Integer.class, String.class)
                        .getAnnotation(RequirePermission.class).value()[0]);
        assertEquals("system:apiKey:add",
                SysApiKeyController.class.getMethod("create", Map.class)
                        .getAnnotation(RequirePermission.class).value()[0]);
        assertEquals("system:apiKey:edit",
                SysApiKeyController.class.getMethod("changeStatus", Long.class, String.class)
                        .getAnnotation(RequirePermission.class).value()[0]);
        assertEquals("system:apiKey:delete",
                SysApiKeyController.class.getMethod("delete", Long.class)
                        .getAnnotation(RequirePermission.class).value()[0]);
    }
}
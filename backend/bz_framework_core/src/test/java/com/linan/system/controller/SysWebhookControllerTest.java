package com.linan.system.controller;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.security.RequirePermission;
import com.linan.system.entity.SysWebhook;
import com.linan.system.service.ISysWebhookService;
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
 * SysWebhookController 单元测试
 *
 * <p>覆盖：新增校验（名称/回调地址/协议）、secret 脱敏（列表与创建响应均为掩码）、
 * @RequirePermission 权限串与 V32 F 菜单一致。</p>
 */
@ExtendWith(MockitoExtension.class)
class SysWebhookControllerTest {

    @Mock
    private ISysWebhookService webhookService;

    @BeforeAll
    static void initTableInfoCache() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), SysWebhook.class);
    }

    private SysWebhookController newController() {
        return new SysWebhookController(webhookService);
    }

    private SysWebhook sampleWebhook() {
        SysWebhook webhook = new SysWebhook();
        webhook.setId(1L);
        webhook.setName("审批通知");
        webhook.setUrl("https://hook.example.com/approval");
        webhook.setSecret("s3cr3t");
        webhook.setEvents("approval.completed");
        webhook.setStatus("0");
        webhook.setCreateTime(LocalDateTime.of(2026, 8, 5, 10, 0));
        return webhook;
    }

    @Test
    @DisplayName("分页列表 secret 一律脱敏为掩码")
    void page_masksSecret() {
        Page<SysWebhook> page = new Page<>(1, 10);
        page.setRecords(List.of(sampleWebhook()));
        page.setTotal(1);
        when(webhookService.page(any(), any())).thenReturn(page);

        Result<?> result = newController().page(1, 10, null);

        assertEquals(200, result.getCode());
        PageResult<Map<String, Object>> pr = (PageResult<Map<String, Object>>) result.getData();
        Map<String, Object> item = pr.getList().get(0);
        assertEquals("******", item.get("secret"), "secret 必须脱敏，不返回明文");
    }

    @Test
    @DisplayName("新增校验：名称/回调地址/事件必填，协议限 http/https")
    void create_validation() {
        SysWebhookController controller = newController();

        assertEquals(500, controller.create(Map.of()).getCode(), "全空必须拒绝");
        assertEquals(500, controller.create(Map.of("name", "x", "url", "ftp://bad", "events", "a")).getCode(),
                "非 http/https 协议必须拒绝");
        assertEquals(500, controller.create(Map.of("name", "x", "url", "https://ok.com")).getCode(),
                "缺事件必须拒绝");
    }

    @Test
    @DisplayName("新增成功 secret 返回掩码")
    void create_success_secretMasked() {
        when(webhookService.save(any(SysWebhook.class))).thenReturn(true);

        Result<?> result = newController().create(
                Map.of("name", "审批通知", "url", "https://hook.example.com", "secret", "s3cr3t", "events", "approval.completed"));

        assertEquals(200, result.getCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertEquals("******", data.get("secret"), "创建响应中的 secret 也必须是掩码");
        assertEquals("0", data.get("status"), "新增默认启用");
    }

    @Test
    @DisplayName("启停/删除接口声明权限串与 V32 F 菜单一致")
    void permissions_alignedWithMenu() throws Exception {
        assertEquals("system:webhook:list",
                SysWebhookController.class.getMethod("page", Integer.class, Integer.class, String.class)
                        .getAnnotation(RequirePermission.class).value()[0]);
        assertEquals("system:webhook:add",
                SysWebhookController.class.getMethod("create", Map.class)
                        .getAnnotation(RequirePermission.class).value()[0]);
        assertEquals("system:webhook:edit",
                SysWebhookController.class.getMethod("changeStatus", Long.class, String.class)
                        .getAnnotation(RequirePermission.class).value()[0]);
        assertEquals("system:webhook:delete",
                SysWebhookController.class.getMethod("delete", Long.class)
                        .getAnnotation(RequirePermission.class).value()[0]);
    }
}
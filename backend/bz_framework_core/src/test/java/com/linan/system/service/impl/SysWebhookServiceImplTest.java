package com.linan.system.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linan.system.entity.SysWebhook;
import com.linan.system.mapper.SysWebhookMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

/**
 * Webhook 订阅服务单元测试（批5）
 *
 * <p>验证两处收敛：
 * <ul>
 *   <li>事件订阅改为精确匹配——events 逗号分隔，取与目标事件相等的项，
 *       规避 {@code like} 导致的「notice」命中「notice.published」类子串误报。</li>
 *   <li>JSON 序列化委托 Jackson，特殊字符（引号/反斜杠/换行）被正确转义，不产生非法 JSON。</li>
 * </ul>
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class SysWebhookServiceImplTest {

    private SysWebhookServiceImpl webhookService;

    @BeforeAll
    static void initTableInfoCache() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), SysWebhook.class);
    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        // 手动构造注入 ObjectMapper；MyBatis-Plus baseMapper 用 mock 反射注入
        webhookService = new SysWebhookServiceImpl(new ObjectMapper());
        SysWebhookMapper mapper = org.mockito.Mockito.mock(SysWebhookMapper.class);
        ReflectionTestUtils.setField(webhookService, "baseMapper", mapper);
    }

    private SysWebhook hook(Long id, String events) {
        SysWebhook w = new SysWebhook();
        w.setId(id);
        w.setName("订阅" + id);
        w.setUrl("https://hook.example.com/" + id);
        w.setEvents(events);
        w.setStatus("0");
        w.setCreateTime(LocalDateTime.of(2026, 8, 5, 10, 0));
        return w;
    }

    /** 拦截基类 list(wrapper) → baseMapper.selectList(wrapper)，返回受控列表 */
    @SuppressWarnings("unchecked")
    private void stubBaseList(java.util.List<SysWebhook> records) {
        SysWebhookMapper mapper = (SysWebhookMapper) ReflectionTestUtils.getField(webhookService, "baseMapper");
        org.mockito.Mockito.when(mapper.selectList(any())).thenReturn(records);
    }

    @Test
    @DisplayName("精确订阅：子串不误匹配（approval 不命中 approval.completed 反向）")
    void listSubscribers_exactEventMatch() {
        stubBaseList(Arrays.asList(
                hook(1L, "approval.completed"),
                hook(2L, "notice.published, approval.completed"),
                hook(3L, "approval"),
                hook(4L, "notice.published")));

        var matched = webhookService.listSubscribers("approval.completed");

        // 精确订阅 approval.completed 的 id 1、2 命中；id 3（approval 非完整事件）不命中
        assertEquals(2, matched.size());
        assertEquals(0, matched.stream().filter(w -> w.getId() == 3L).count());
        assertEquals(0, matched.stream().filter(w -> w.getId() == 4L).count());
    }

    @Test
    @DisplayName("精确订阅：事件项带前后空格 trim 后仍精确匹配")
    void listSubscribers_trimsEventItems() {
        stubBaseList(Arrays.asList(
                hook(1L, " approval.completed , notice.published "),
                hook(2L, "notice.published")));

        var lib = webhookService.listSubscribers("approval.completed");

        assertEquals(1, lib.size());
        assertEquals(1L, lib.get(0).getId());
    }

    @Test
    @DisplayName("查询只取启用订阅：wrapper 携带 status = '0' 条件")
    void listSubscribers_ignoresDisabled() {
        stubBaseList(Arrays.asList(hook(6L, "approval.completed")));
        org.mockito.ArgumentCaptor<Object> captor =
                org.mockito.ArgumentCaptor.forClass(Object.class);

        webhookService.listSubscribers("approval.completed");

        // 校验发给 baseMapper 的 wrapper 在 SQL 层约束仅启用订阅
        org.mockito.ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.Wrapper> wc =
                org.mockito.ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.Wrapper.class);
        SysWebhookMapper mapper = (SysWebhookMapper) ReflectionTestUtils.getField(webhookService, "baseMapper");
        org.mockito.Mockito.verify(mapper).selectList(wc.capture());
        com.baomidou.mybatisplus.core.conditions.AbstractWrapper<?, ?, ?> aw =
                (com.baomidou.mybatisplus.core.conditions.AbstractWrapper<?, ?, ?>) wc.getValue();
        // eq 生成 status 等值条件 + 参数绑定 0
        assertTrue(aw.getSqlSegment().toLowerCase().contains("status ="),
                "应生成 status 等值条件，实际: " + aw.getSqlSegment());
        assertTrue(aw.getParamNameValuePairs().containsValue("0"),
                "status 条件必须绑定启用值 0，实际: " + aw.getParamNameValuePairs());
    }

    @Test
    @DisplayName("JSON 序列化：引号/反斜杠/换行由 Jackson 正确转义且值保全")
    void toJson_escapesSpecialChars() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("title", "值含\"双引号\"");
        payload.put("content", "首行\n次行含\\反斜杠");
        payload.put("n", 42);
        payload.put("none", null);

        Map<String, Object> body = new HashMap<>();
        body.put("event", "approval.completed");
        body.put("eventType", "审批完成");
        body.put("timestamp", 1L);
        body.put("data", payload);

        java.lang.reflect.Method m =
                SysWebhookServiceImpl.class.getDeclaredMethod("toJson", java.util.Map.class);
        m.setAccessible(true);
        String json = (String) m.invoke(webhookService, body);

        // 反序列化反向校验：JSON 合法且特殊字符保全
        var parsed = new ObjectMapper().readTree(json);
        assertEquals("审批完成", parsed.get("eventType").asText());
        assertEquals("值含\"双引号\"", parsed.get("data").get("title").asText());
        assertEquals("首行\n次行含\\反斜杠", parsed.get("data").get("content").asText());
        assertEquals(42, parsed.get("data").get("n").asInt());
        assertTrue(parsed.get("data").get("none").isNull());
    }
}
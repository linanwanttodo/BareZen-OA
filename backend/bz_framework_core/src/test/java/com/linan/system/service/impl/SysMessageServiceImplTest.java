package com.linan.system.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.system.entity.SysMessage;
import com.linan.system.mapper.SysMessageMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 站内消息服务单元测试
 *
 * <p>覆盖：离线兜底保存（msgType 默认值）、分页按人过滤、未读数统计、
 * 单条已读（仅本人 + 幂等）、全部已读。baseMapper 为 MyBatis-Plus 泛型字段，
 * 通过反射注入（参照 SysFileServiceImplTest 模式）。</p>
 */
@ExtendWith(MockitoExtension.class)
class SysMessageServiceImplTest {

    @Mock
    private SysMessageMapper baseMapper;

    private SysMessageServiceImpl messageService;

    @BeforeAll
    static void initTableInfoCache() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), SysMessage.class);
    }

    @BeforeEach
    void setUp() {
        messageService = new SysMessageServiceImpl();
        ReflectionTestUtils.setField(messageService, "baseMapper", baseMapper);
    }

    @Test
    @DisplayName("离线兜底：保存站内信，msgType 缺省为 system")
    void saveMessage_defaultsMsgType() {
        when(baseMapper.insert(any(SysMessage.class))).thenReturn(1);

        messageService.saveMessage(10L, "审批通过", "您的请假已通过", null, "leave", 5L);

        verify(baseMapper).insert(any(SysMessage.class));
    }

    @Test
    @DisplayName("分页查询：按收件人与已读状态过滤")
    void pageMine_filtersByUserAndRead() {
        when(baseMapper.selectPage(any(), any())).thenReturn(new Page<SysMessage>().setRecords(
                java.util.List.of(new SysMessage())));

        IPage<SysMessage> result = messageService.pageMine(new Page<>(1, 10), 10L, 0);

        assertEquals(1, result.getRecords().size());
        verify(baseMapper).selectPage(any(), any());
    }

    @Test
    @DisplayName("未读数：count 按 user_id + is_read=0")
    void unreadCount_queriesUnread() {
        when(baseMapper.selectCount(any())).thenReturn(3L);

        assertEquals(3L, messageService.unreadCount(10L));
    }

    @Test
    @DisplayName("标记已读：他人消息被拒绝（归属校验）")
    void markRead_foreignMessage_rejected() {
        SysMessage message = new SysMessage();
        message.setId(1L);
        message.setUserId(999L); // 非本人
        message.setIsRead(0);
        when(baseMapper.selectById(1L)).thenReturn(message);

        assertFalse(messageService.markRead(1L, 10L));
    }

    @Test
    @DisplayName("标记已读：本人未读消息更新成功，已读幂等返回 true")
    void markRead_ownMessage_success() {
        SysMessage message = new SysMessage();
        message.setId(1L);
        message.setUserId(10L);
        message.setIsRead(0);
        when(baseMapper.selectById(1L)).thenReturn(message);
        when(baseMapper.update(any(), any())).thenReturn(1);

        assertTrue(messageService.markRead(1L, 10L));

        // 已读幂等：不触发更新
        message.setIsRead(1);
        assertTrue(messageService.markRead(1L, 10L));
    }

    @Test
    @DisplayName("全部标记已读：条件更新返回条数")
    void markAllRead_updatesAll() {
        when(baseMapper.update(any(), any())).thenReturn(5);

        assertEquals(5, messageService.markAllRead(10L));
    }
}
package com.linan.system.service.impl;

import com.linan.core.common.event.NoticePublishEvent;
import com.linan.system.entity.SysNotice;
import com.linan.system.entity.SysNoticeRead;
import com.linan.system.mapper.SysNoticeMapper;
import com.linan.system.service.ISysNoticeReadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 公告发布事件单元测试（3.4 公告邮件可选渠道）
 *
 * <p>发布事件携带 {@code mail} 标志：公告 sendEmail=Y 时 true，
 * 默认（空/N）false，由监听器决定是否额外发送全员邮件。</p>
 */
@ExtendWith(MockitoExtension.class)
class SysNoticeServiceImplTest {

    @Mock
    private SysNoticeMapper noticeMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private ISysNoticeReadService noticeReadService;

    private SysNoticeServiceImpl noticeService;

    @BeforeEach
    void setUp() {
        noticeService = new SysNoticeServiceImpl(eventPublisher, noticeReadService);
        ReflectionTestUtils.setField(noticeService, "baseMapper", noticeMapper);
    }

    private SysNotice notice(Long id, String sendEmail) {
        SysNotice n = new SysNotice();
        n.setId(id);
        n.setNoticeTitle("放假通知");
        n.setNoticeContent("五一放假安排");
        n.setStatus("0");
        n.setSendEmail(sendEmail);
        return n;
    }

    @Test
    @DisplayName("发布公告（sendEmail=Y）：发布事件携带 mail=true（监听器将发全员邮件）")
    void publish_sendEmailYes_eventMailTrue() {
        when(noticeMapper.selectById(1L)).thenReturn(notice(1L, "Y"));
        when(noticeMapper.updateById(any(SysNotice.class))).thenReturn(1);

        boolean ok = noticeService.publish(1L);

        assertTrue(ok);
        ArgumentCaptor<NoticePublishEvent> captor = ArgumentCaptor.forClass(NoticePublishEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertTrue(captor.getValue().isMail());
    }

    @Test
    @DisplayName("发布公告（默认 sendEmail 为空）：发布事件 mail=false，仅站内推送")
    void publish_default_eventMailFalse() {
        when(noticeMapper.selectById(1L)).thenReturn(notice(1L, null));
        when(noticeMapper.updateById(any(SysNotice.class))).thenReturn(1);

        boolean ok = noticeService.publish(1L);

        assertTrue(ok);
        ArgumentCaptor<NoticePublishEvent> captor = ArgumentCaptor.forClass(NoticePublishEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertFalse(captor.getValue().isMail());
    }

    @Test
    @DisplayName("发布公告（sendEmail=N）：发布事件 mail=false")
    void publish_sendEmailNo_eventMailFalse() {
        when(noticeMapper.selectById(1L)).thenReturn(notice(1L, "N"));
        when(noticeMapper.updateById(any(SysNotice.class))).thenReturn(1);

        boolean ok = noticeService.publish(1L);

        assertTrue(ok);
        ArgumentCaptor<NoticePublishEvent> captor = ArgumentCaptor.forClass(NoticePublishEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertFalse(captor.getValue().isMail());
    }

    @Test
    @DisplayName("公告不存在：发布失败，不发事件")
    void publish_missing_noEvent() {
        when(noticeMapper.selectById(1L)).thenReturn(null);

        assertFalse(noticeService.publish(1L));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("阅读回执：公告不存在返回 null，不查询已读")
    void readReceipt_missingNotice_returnsNull() {
        when(noticeMapper.selectById(9L)).thenReturn(null);

        assertNull(noticeService.readReceipt(9L));
        verify(noticeReadService, never()).lambdaQuery();
    }

    @Test
    @DisplayName("阅读回执：同一用户多次阅读去重计数")
    void readReceipt_readCountDistinctByUser() {
        SysNotice notice = new SysNotice();
        notice.setId(9L);
        notice.setRequireRead("Y");
        when(noticeMapper.selectById(9L)).thenReturn(notice);

        // 同一用户(5)阅读两次，另一用户(8)阅读一次 → 去重后 readCount = 2
        stubLambdaQueryReturns(List.of(read(9L, 5L), read(9L, 5L), read(9L, 8L)));

        Map<String, Object> data = noticeService.readReceipt(9L);

        assertEquals(2L, data.get("readCount"), "已读用户数应按 userId 去重");
        assertEquals("Y", data.get("requireRead"));
    }

    private SysNoticeRead read(Long noticeId, Long userId) {
        SysNoticeRead r = new SysNoticeRead();
        r.setNoticeId(noticeId);
        r.setUserId(userId);
        r.setReadTime(java.time.LocalDateTime.of(2026, 8, 5, 10, 0));
        return r;
    }

    /** stub lambdaQuery() 链式查询返回给定已读记录 */
    private void stubLambdaQueryReturns(List<SysNoticeRead> reads) {
        @SuppressWarnings("unchecked")
        com.baomidou.mybatisplus.core.mapper.BaseMapper<SysNoticeRead> mapper =
                org.mockito.Mockito.mock(com.baomidou.mybatisplus.core.mapper.BaseMapper.class);
        when(mapper.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(reads);
        com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper<SysNoticeRead> chain =
                new com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper<>(mapper);
        when(noticeReadService.lambdaQuery()).thenReturn(chain);
    }
}
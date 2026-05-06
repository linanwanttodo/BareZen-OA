package com.linan.system.service.impl;

import com.linan.core.common.event.NoticePublishEvent;
import com.linan.system.entity.SysNotice;
import com.linan.system.mapper.SysNoticeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
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

    private SysNoticeServiceImpl noticeService;

    @BeforeEach
    void setUp() {
        noticeService = new SysNoticeServiceImpl(eventPublisher);
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
}
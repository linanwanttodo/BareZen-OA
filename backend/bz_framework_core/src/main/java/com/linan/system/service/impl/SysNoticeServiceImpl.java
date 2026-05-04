package com.linan.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.core.common.event.NoticePublishEvent;
import com.linan.system.entity.SysNotice;
import com.linan.system.mapper.SysNoticeMapper;
import com.linan.system.service.ISysNoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SysNoticeServiceImpl extends ServiceImpl<SysNoticeMapper, SysNotice> implements ISysNoticeService {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public SysNotice selectLatestNotice() {
        return baseMapper.selectLatestNotice();
    }

    @Override
    public List<SysNotice> selectPublishedNotices(int limit) {
        return baseMapper.selectPublishedNotices(limit);
    }

    @Override
    public boolean publish(Long id) {
        SysNotice notice = getById(id);
        if (notice == null || !"0".equals(notice.getStatus())) {
            return false;
        }
        notice.setStatus("1");
        boolean updated = updateById(notice);
        if (updated) {
            // 发布成功后发布领域事件，由 Web 模块监听并推送 WebSocket 实时通知
            eventPublisher.publishEvent(new NoticePublishEvent(notice.getNoticeTitle(), notice.getNoticeContent()));
        }
        return updated;
    }

    @Override
    public boolean revoke(Long id) {
        SysNotice notice = getById(id);
        if (notice == null || !"1".equals(notice.getStatus())) {
            return false;
        }
        notice.setStatus("2");
        return updateById(notice);
    }
}
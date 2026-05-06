package com.linan.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.core.common.event.NoticePublishEvent;
import com.linan.system.entity.SysNotice;
import com.linan.system.entity.SysNoticeRead;
import com.linan.system.mapper.SysNoticeMapper;
import com.linan.system.service.ISysNoticeReadService;
import com.linan.system.service.ISysNoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysNoticeServiceImpl extends ServiceImpl<SysNoticeMapper, SysNotice> implements ISysNoticeService {

    private final ApplicationEventPublisher eventPublisher;

    private final ISysNoticeReadService noticeReadService;

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
            // 发布成功后发布领域事件，由 Web 模块监听并推送 WebSocket 实时通知；
            // 邮件为可选渠道：仅当公告选择 sendEmail=Y 时携带 mail=true
            eventPublisher.publishEvent(new NoticePublishEvent(
                    notice.getNoticeTitle(), notice.getNoticeContent(), "Y".equals(notice.getSendEmail())));
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

    @Override
    public Map<String, Object> readReceipt(Long id) {
        SysNotice notice = getById(id);
        if (notice == null) {
            return null;
        }
        // 已读记录（按阅读时间倒序）
        List<SysNoticeRead> reads = noticeReadService.lambdaQuery()
                .eq(SysNoticeRead::getNoticeId, id)
                .orderByDesc(SysNoticeRead::getReadTime)
                .list();
        // 已读用户数（去重）
        long readCount = reads.stream()
                .map(SysNoticeRead::getUserId)
                .distinct().count();

        Map<String, Object> data = new HashMap<>();
        data.put("noticeId", id);
        data.put("requireRead", notice.getRequireRead());
        data.put("readCount", readCount);
        data.put("totalCount", null); // 总用户数由前端从用户列表获取，此处仅返回已读
        data.put("reads", reads.stream().map(r -> Map.of(
                "userId", r.getUserId(),
                "readTime", r.getReadTime() != null ? r.getReadTime().toString() : null
        )).collect(Collectors.toList()));
        return data;
    }
}
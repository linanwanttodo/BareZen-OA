package com.linan.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.system.entity.SysMessage;
import com.linan.system.mapper.SysMessageMapper;
import com.linan.system.service.ISysMessageService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 站内消息 Service 实现
 */
@Service
public class SysMessageServiceImpl extends ServiceImpl<SysMessageMapper, SysMessage> implements ISysMessageService {

    @Override
    public void saveMessage(Long userId, String title, String content, String msgType, String bizType, Long bizId) {
        SysMessage message = new SysMessage();
        message.setUserId(userId);
        message.setTitle(title);
        message.setContent(content);
        message.setMsgType(StringUtils.hasText(msgType) ? msgType : "system");
        message.setBizType(bizType);
        message.setBizId(bizId);
        message.setIsRead(0);
        this.save(message);
    }

    @Override
    public IPage<SysMessage> pageMine(Page<SysMessage> page, Long userId, Integer isRead) {
        LambdaQueryWrapper<SysMessage> wrapper = new LambdaQueryWrapper<SysMessage>()
                .eq(SysMessage::getUserId, userId)
                .orderByDesc(SysMessage::getCreateTime);
        if (isRead != null) {
            wrapper.eq(SysMessage::getIsRead, isRead);
        }
        return this.page(page, wrapper);
    }

    @Override
    public long unreadCount(Long userId) {
        return this.count(new LambdaQueryWrapper<SysMessage>()
                .eq(SysMessage::getUserId, userId)
                .eq(SysMessage::getIsRead, 0));
    }

    @Override
    public boolean markRead(Long id, Long userId) {
        SysMessage message = this.getById(id);
        if (message == null || !message.getUserId().equals(userId)) {
            return false;
        }
        if (message.getIsRead() != null && message.getIsRead() == 1) {
            return true; // 已读幂等
        }
        return this.update(new LambdaUpdateWrapper<SysMessage>()
                .eq(SysMessage::getId, id)
                .eq(SysMessage::getIsRead, 0)  // 条件更新防并发重复
                .set(SysMessage::getIsRead, 1)
                .set(SysMessage::getReadTime, LocalDateTime.now()));
    }

    @Override
    public int markAllRead(Long userId) {
        return baseMapper.update(null, new LambdaUpdateWrapper<SysMessage>()
                .eq(SysMessage::getUserId, userId)
                .eq(SysMessage::getIsRead, 0)
                .set(SysMessage::getIsRead, 1)
                .set(SysMessage::getReadTime, LocalDateTime.now()));
    }
}
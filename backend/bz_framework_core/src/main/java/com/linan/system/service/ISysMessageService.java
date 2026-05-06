package com.linan.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.linan.system.entity.SysMessage;

/**
 * 站内消息 Service
 */
public interface ISysMessageService extends IService<SysMessage> {

    /**
     * 保存站内信（WebSocket 离线兜底）
     *
     * @param userId  收件人
     * @param title   标题
     * @param content 内容
     * @param msgType 消息类型（approval/system/notice）
     * @param bizType 关联业务类型（可空）
     * @param bizId   关联业务单ID（可空）
     */
    void saveMessage(Long userId, String title, String content, String msgType, String bizType, Long bizId);

    /**
     * 分页查询某用户的站内信（按创建时间倒序）
     *
     * @param isRead 可空：null=全部 0=未读 1=已读
     */
    IPage<SysMessage> pageMine(Page<SysMessage> page, Long userId, Integer isRead);

    /**
     * 未读数量
     */
    long unreadCount(Long userId);

    /**
     * 标记单条已读（仅限本人消息）
     *
     * @return 是否找到并更新
     */
    boolean markRead(Long id, Long userId);

    /**
     * 全部标记已读
     *
     * @return 更新条数
     */
    int markAllRead(Long userId);
}
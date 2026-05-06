package com.linan.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.linan.system.entity.SysNotice;

public interface ISysNoticeService extends IService<SysNotice> {

    SysNotice selectLatestNotice();

    java.util.List<SysNotice> selectPublishedNotices(int limit);

    boolean publish(Long id);

    boolean revoke(Long id);

    /**
     * 公告阅读回执统计：已读/未读人数 + 已读明细（按阅读时间倒序）
     *
     * @param id 公告ID
     * @return 统计结果 Map；公告不存在时返回 null
     */
    java.util.Map<String, Object> readReceipt(Long id);
}
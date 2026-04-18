package com.linan.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.linan.system.entity.SysNotice;

public interface ISysNoticeService extends IService<SysNotice> {

    SysNotice selectLatestNotice();

    java.util.List<SysNotice> selectPublishedNotices(int limit);

    boolean publish(Long id);

    boolean revoke(Long id);
}
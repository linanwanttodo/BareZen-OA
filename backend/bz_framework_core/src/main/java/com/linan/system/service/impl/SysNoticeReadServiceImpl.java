package com.linan.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.system.entity.SysNoticeRead;
import com.linan.system.mapper.SysNoticeReadMapper;
import com.linan.system.service.ISysNoticeReadService;
import org.springframework.stereotype.Service;

/**
 * 公告已读记录服务实现
 *
 * @author BareZen Team
 */
@Service
public class SysNoticeReadServiceImpl extends ServiceImpl<SysNoticeReadMapper, SysNoticeRead>
        implements ISysNoticeReadService {
}

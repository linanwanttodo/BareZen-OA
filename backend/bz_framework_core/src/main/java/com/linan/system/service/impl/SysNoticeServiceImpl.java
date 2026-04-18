package com.linan.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.system.entity.SysNotice;
import com.linan.system.mapper.SysNoticeMapper;
import com.linan.system.service.ISysNoticeService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysNoticeServiceImpl extends ServiceImpl<SysNoticeMapper, SysNotice> implements ISysNoticeService {

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
        return updateById(notice);
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
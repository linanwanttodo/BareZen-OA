package com.linan.system.controller;

import com.linan.core.common.api.Result;
import com.linan.core.common.controller.BaseController;
import com.linan.core.security.RequirePermission;
import com.linan.system.entity.SysNotice;
import com.linan.system.service.ISysNoticeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system/v1/notices")
public class SysNoticeController extends BaseController<SysNotice, ISysNoticeService> {
    @Autowired private ISysNoticeService noticeService;
    @Override protected ISysNoticeService getService() { return noticeService; }

    @RequirePermission("system:notice:list")
    @GetMapping("/latest")
    public Result<SysNotice> getLatest() {
        SysNotice notice = noticeService.selectLatestNotice();
        return Result.success(notice);
    }

    @RequirePermission("system:notice:list")
    @GetMapping("/published")
    public Result<List<SysNotice>> getPublished() {
        List<SysNotice> list = noticeService.selectPublishedNotices(10);
        return Result.success(list);
    }

    @RequirePermission("system:notice:edit")
    @PutMapping("/publish/{id}")
    public Result<Void> publish(@PathVariable Long id) {
        boolean ok = noticeService.publish(id);
        return ok ? Result.success(null) : Result.error("发布失败");
    }

    @RequirePermission("system:notice:edit")
    @PutMapping("/revoke/{id}")
    public Result<Void> revoke(@PathVariable Long id) {
        boolean ok = noticeService.revoke(id);
        return ok ? Result.success(null) : Result.error("下架失败");
    }
}
package com.linan.system.controller;

import com.linan.core.common.annotation.OperLog;
import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.common.controller.BaseController;
import com.linan.core.security.RequirePermission;
import com.linan.system.entity.SysNotice;
import com.linan.system.service.ISysNoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "通知公告", description = "通知公告的 CRUD 与发布管理")
@RestController
@RequestMapping("/api/system/v1/notices")
@RequiredArgsConstructor
public class SysNoticeController extends BaseController<SysNotice, ISysNoticeService> {
    private final ISysNoticeService noticeService;
    @Override protected ISysNoticeService getService() { return noticeService; }

    @RequirePermission("system:notice:list")
    @Operation(summary = "分页查询通知公告")
    @GetMapping("/page")
    @Override
    public Result<PageResult<SysNotice>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return super.list(pageNum, pageSize);
    }

    @RequirePermission("system:notice:list")
    @Operation(summary = "查询通知公告详情")
    @GetMapping("/{id}")
    @Override
    public Result<SysNotice> getById(@PathVariable Long id) {
        return super.getById(id);
    }

    @Operation(summary = "新增通知公告")
    @OperLog(title = "通知公告", businessType = 1)
    @RequirePermission("system:notice:add")
    @PostMapping
    @Override
    public Result<Void> save(@jakarta.validation.Valid @RequestBody SysNotice entity) {
        return super.save(entity);
    }

    @Operation(summary = "更新通知公告")
    @OperLog(title = "通知公告", businessType = 2)
    @RequirePermission("system:notice:edit")
    @PutMapping
    @Override
    public Result<Void> update(@jakarta.validation.Valid @RequestBody SysNotice entity) {
        return super.update(entity);
    }

    @Operation(summary = "删除通知公告")
    @OperLog(title = "通知公告", businessType = 3)
    @RequirePermission("system:notice:delete")
    @DeleteMapping("/{id}")
    @Override
    public Result<Void> delete(@PathVariable Long id) {
        return super.delete(id);
    }

    @RequirePermission("system:notice:list")
    @Operation(summary = "获取最新通知公告")
    @GetMapping("/latest")
    public Result<SysNotice> getLatest() {
        SysNotice notice = noticeService.selectLatestNotice();
        return Result.success(notice);
    }

    @RequirePermission("system:notice:list")
    @Operation(summary = "获取已发布的通知公告列表")
    @GetMapping("/published")
    public Result<List<SysNotice>> getPublished() {
        List<SysNotice> list = noticeService.selectPublishedNotices(10);
        return Result.success(list);
    }

    @RequirePermission("system:notice:edit")
    @Operation(summary = "发布通知公告")
    @PutMapping("/publish/{id}")
    public Result<Void> publish(@PathVariable Long id) {
        boolean ok = noticeService.publish(id);
        return ok ? Result.success(null) : Result.error("发布失败");
    }

    @RequirePermission("system:notice:edit")
    @Operation(summary = "下架通知公告")
    @PutMapping("/revoke/{id}")
    public Result<Void> revoke(@PathVariable Long id) {
        boolean ok = noticeService.revoke(id);
        return ok ? Result.success(null) : Result.error("下架失败");
    }

    /**
     * 公告阅读回执统计：已读/未读人数 + 已读明细
     * GET /api/system/v1/notices/{id}/read-receipt
     */
    @RequirePermission("system:notice:list")
    @Operation(summary = "公告阅读回执统计")
    @GetMapping("/{id}/read-receipt")
    public Result<Map<String, Object>> readReceipt(@PathVariable Long id) {
        Map<String, Object> data = noticeService.readReceipt(id);
        return data == null ? Result.error("公告不存在") : Result.success(data);
    }
}

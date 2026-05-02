package com.linan.kb.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.annotation.OperLog;
import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.security.RequirePermission;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.kb.entity.KbDocument;
import com.linan.kb.service.IKbDocumentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 知识文档 Controller
 *
 * <p>支持分类查询、发布/下线、浏览计数、CRUD。</p>
 *
 * @author BareZen Team
 */
@Tag(name = "知识文档", description = "知识库 - 文档沉淀")
@RestController
@RequestMapping("/api/kb/v1/documents")
public class KbDocumentController {

    @Autowired
    private IKbDocumentService documentService;

    /**
     * 条件分页查询
     */
    @RequirePermission("kb:document:list")
    @Operation(summary = "分页查询知识文档")
    @GetMapping("/page")
    public Result<PageResult<KbDocument>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status) {

        Page<KbDocument> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<KbDocument> wrapper = new LambdaQueryWrapper<>();

        if (title != null && !title.isEmpty()) {
            wrapper.like(KbDocument::getTitle, title);
        }
        if (category != null && !category.isEmpty()) {
            wrapper.eq(KbDocument::getCategory, category);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(KbDocument::getStatus, status);
        }
        wrapper.orderByDesc(KbDocument::getCreateTime);
        IPage<KbDocument> result = documentService.page(page, wrapper);
        return Result.page(result);
    }

    /**
     * 新增文档（作者自动填充为当前登录用户）
     */
    @Operation(summary = "新增知识文档")
    @OperLog(title = "知识文档", businessType = 1)
    @RequirePermission("kb:document:add")
    @PostMapping
    public Result<Void> save(@Valid @RequestBody KbDocument doc) {
        if (doc.getStatus() == null || doc.getStatus().isEmpty()) {
            doc.setStatus("0");
        }
        Long userId = AuthInterceptor.getCurrentUserId();
        doc.setAuthorId(userId);
        if (doc.getAuthorName() == null || doc.getAuthorName().isEmpty()) {
            doc.setAuthorName(userId != null ? String.valueOf(userId) : null);
        }
        if (doc.getViewCount() == null) {
            doc.setViewCount(0);
        }
        boolean ok = documentService.save(doc);
        return ok ? Result.success(null) : Result.error("新增失败");
    }

    /**
     * 更新文档
     */
    @Operation(summary = "修改知识文档")
    @OperLog(title = "知识文档", businessType = 2)
    @RequirePermission("kb:document:edit")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody KbDocument doc) {
        boolean ok = documentService.updateById(doc);
        return ok ? Result.success(null) : Result.error("更新失败");
    }

    /**
     * 删除文档
     */
    @Operation(summary = "删除知识文档")
    @OperLog(title = "知识文档", businessType = 3)
    @RequirePermission("kb:document:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        boolean ok = documentService.removeById(id);
        return ok ? Result.success(null) : Result.error("删除失败");
    }

    /**
     * 文档详情（浏览时自动 +1 浏览量）
     */
    @RequirePermission("kb:document:list")
    @Operation(summary = "查询文档详情")
    @GetMapping("/{id}")
    public Result<KbDocument> getById(@PathVariable Long id) {
        KbDocument doc = documentService.getById(id);
        if (doc == null) {
            return Result.error("文档不存在");
        }
        // 浏览量 +1（幂等，仅统计）
        documentService.lambdaUpdate()
                .eq(KbDocument::getId, id)
                .setSql("view_count = view_count + 1")
                .update();
        doc.setViewCount(doc.getViewCount() != null ? doc.getViewCount() + 1 : 1);
        return Result.success(doc);
    }

    /**
     * 发布/下线文档
     */
    @Operation(summary = "发布/下线文档")
    @OperLog(title = "知识文档", businessType = 2)
    @RequirePermission("kb:document:edit")
    @PutMapping("/{id}/status/{status}")
    public Result<Void> changeStatus(@PathVariable Long id, @PathVariable String status) {
        KbDocument doc = new KbDocument();
        doc.setId(id);
        doc.setStatus(status);
        boolean ok = documentService.updateById(doc);
        return ok ? Result.success(null) : Result.error("状态切换失败");
    }
}

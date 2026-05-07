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
import com.linan.kb.entity.KbDocumentVersion;
import com.linan.kb.service.IKbDocumentService;
import com.linan.kb.service.IKbDocumentVersionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

/**
 * 知识文档 Controller
 *
 * <p>仅做参数绑定与 {@link Result} 委托；版本快照、作者兜底、浏览量自增、删除双轨校验
 * 等规则下沉至 {@link IKbDocumentService}。</p>
 *
 * @author BareZen Team
 */
@Tag(name = "知识文档", description = "知识库 - 文档沉淀")
@RestController
@RequestMapping("/api/kb/v1/documents")
@RequiredArgsConstructor
public class KbDocumentController {

    private final IKbDocumentService documentService;

    private final IKbDocumentVersionService versionService;

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
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status) {

        Page<KbDocument> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<KbDocument> wrapper = new LambdaQueryWrapper<>();
        if (title != null && !title.isEmpty()) {
            wrapper.like(KbDocument::getTitle, title);
        }
        if (category != null && !category.isEmpty()) {
            wrapper.eq(KbDocument::getCategory, category);
        }
        if (categoryId != null) {
            wrapper.eq(KbDocument::getCategoryId, categoryId);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(KbDocument::getStatus, status);
        }
        wrapper.orderByDesc(KbDocument::getCreateTime);
        IPage<KbDocument> result = documentService.page(page, wrapper);
        return Result.page(result);
    }

    /**
     * 新增文档（作者自动填充为当前登录用户；首版即生成版本 v1）
     */
    @Operation(summary = "新增知识文档")
    @OperLog(title = "知识文档", businessType = 1)
    @RequirePermission("kb:document:add")
    @PostMapping
    public Result<Void> save(@Valid @RequestBody KbDocument doc) {
        boolean ok = documentService.createDocument(doc);
        return ok ? Result.success(null) : Result.error("新增失败");
    }

    /**
     * 更新文档（更新前先写当前内容版本快照，保证历史可回溯）
     */
    @Operation(summary = "修改知识文档")
    @OperLog(title = "知识文档", businessType = 2)
    @RequirePermission("kb:document:edit")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody KbDocument doc) {
        documentService.updateDocument(doc);
        return Result.success(null);
    }

    /**
     * 文档版本历史（version_no 倒序，最新在前）
     */
    @Operation(summary = "查询文档版本历史")
    @RequirePermission("kb:document:list")
    @GetMapping("/{id}/versions")
    public Result<List<KbDocumentVersion>> versions(@PathVariable Long id) {
        return Result.success(versionService.list(new LambdaQueryWrapper<KbDocumentVersion>()
                .eq(KbDocumentVersion::getDocId, id)
                .orderByDesc(KbDocumentVersion::getVersionNo)));
    }

    /**
     * 删除文档（作者本人 OR 持删除权限，双轨校验在 service 内进行）
     */
    @Operation(summary = "删除知识文档")
    @OperLog(title = "知识文档", businessType = 3)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        documentService.deleteDocument(id, userId);
        return Result.success(null);
    }

    /**
     * 文档详情（浏览时自动 +1 浏览量）
     */
    @RequirePermission("kb:document:list")
    @Operation(summary = "查询文档详情")
    @GetMapping("/{id}")
    public Result<KbDocument> getById(@PathVariable Long id) {
        return Result.success(documentService.viewDocument(id));
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
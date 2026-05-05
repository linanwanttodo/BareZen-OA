package com.linan.kb.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.annotation.OperLog;
import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.security.PermissionService;
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
 * <p>支持分类查询、发布/下线、浏览计数、CRUD。</p>
 *
 * @author BareZen Team
 */
@Tag(name = "知识文档", description = "知识库 - 文档沉淀")
@RestController
@RequestMapping("/api/kb/v1/documents")
@RequiredArgsConstructor
public class KbDocumentController {

    private final IKbDocumentService documentService;

    private final PermissionService permissionService;

    /** 文档版本（V41：更新前写快照） */
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
        if (doc.getStatus() == null || doc.getStatus().isEmpty()) {
            doc.setStatus("0");
        }
        Long userId = AuthInterceptor.getCurrentUserId();
        doc.setAuthorId(userId);
        if (doc.getAuthorName() == null || doc.getAuthorName().isEmpty()) {
            // 后端兜底填真实用户名（JWT 中解析），禁止把 userId 当姓名落库
            doc.setAuthorName(userId != null ? AuthInterceptor.getCurrentUsername() : null);
        }
        if (doc.getViewCount() == null) {
            doc.setViewCount(0);
        }
        boolean ok = documentService.save(doc);
        if (ok) {
            // 首版快照（v1）
            writeVersion(doc, userId);
        }
        return ok ? Result.success(null) : Result.error("新增失败");
    }

    /**
     * 更新文档（更新前先写当前内容为版本快照，保证历史可回溯）
     */
    @Operation(summary = "修改知识文档")
    @OperLog(title = "知识文档", businessType = 2)
    @RequirePermission("kb:document:edit")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody KbDocument doc) {
        KbDocument old = documentService.getById(doc.getId());
        if (old == null) {
            return Result.error("文档不存在");
        }
        // 更新前快照：写入旧内容副本（operator 为当前用户）
        Long userId = AuthInterceptor.getCurrentUserId();
        writeVersion(old, userId);
        boolean ok = documentService.updateById(doc);
        return ok ? Result.success(null) : Result.error("更新失败");
    }

    /**
     * 文档版本历史（version_no 倒序，最新在前）
     * GET /api/kb/v1/documents/{id}/versions
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
     * 写版本快照：version_no = 现有最大 + 1（首版为 1）
     */
    private void writeVersion(KbDocument doc, Long operatorId) {
        KbDocumentVersion version = new KbDocumentVersion();
        version.setDocId(doc.getId());
        version.setTitle(doc.getTitle());
        version.setCategoryId(doc.getCategoryId());
        version.setContent(doc.getContent());
        version.setOperatorId(operatorId);
        version.setOperatorName(operatorId != null ? AuthInterceptor.getCurrentUsername() : null);
        version.setVersionNo(nextVersionNo(doc.getId()));
        versionService.save(version);
    }

    /** 文档当前最大版本号 + 1（无历史时从 1 开始） */
    private int nextVersionNo(Long docId) {
        List<KbDocumentVersion> versions = versionService.list(new LambdaQueryWrapper<KbDocumentVersion>()
                .eq(KbDocumentVersion::getDocId, docId)
                .orderByDesc(KbDocumentVersion::getVersionNo)
                .last("LIMIT 1"));
        return versions.isEmpty() ? 1 : versions.get(0).getVersionNo() + 1;
    }

    /**
     * 删除文档（作者本人 OR 持删除权限，双轨校验）
     *
     * <p>移除方法级 @RequirePermission：否则作者本人（无删除按钮权限）会被切面先行拦截，
     * 无法删除自己的文档；改为方法内 isOwner || hasPermission 双轨放行。</p>
     */
    @Operation(summary = "删除知识文档")
    @OperLog(title = "知识文档", businessType = 3)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        KbDocument doc = documentService.getById(id);
        if (doc == null) {
            return Result.error("文档不存在");
        }
        boolean isOwner = userId.equals(doc.getAuthorId());
        boolean isAdmin = permissionService.hasPermission(userId, "kb:document:delete");
        if (!isOwner && !isAdmin) {
            return Result.error("无权限删除该文档");
        }
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

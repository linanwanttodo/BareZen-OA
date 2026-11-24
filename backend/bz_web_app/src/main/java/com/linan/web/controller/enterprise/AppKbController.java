package com.linan.web.controller.enterprise;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.api.PageParamGuard;
import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.security.PermissionService;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.kb.entity.KbCategory;
import com.linan.kb.entity.KbDocument;
import com.linan.kb.service.IKbCategoryService;
import com.linan.kb.service.IKbDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 知识库只读浏览 Controller（用户前台 /api/app/v1/kb）
 *
 * <p>员工自助浏览知识分类与已发布文档。与管理后台的知识文档接口（/api/kb/v1/documents）
 * 分离：这里仅提供只读查询，查询条件由服务端强制注入「已发布」状态过滤，
 * 接口不接收 status 参数，草稿文档对非管理员一律不可见。</p>
 *
 * <p>分类列表与文档查询复用 {@link IKbCategoryService} / {@link IKbDocumentService}，
 * 详情浏览量自增复用 {@link IKbDocumentService#viewDocument} 的原子自增
 * （setSql view_count = view_count + 1，并发安全）。</p>
 *
 * @author BareZen Team
 */
@Tag(name = "知识库", description = "用户前台 - 知识库只读浏览")
@RestController
@RequestMapping("/api/app/v1/kb")
@RequiredArgsConstructor
public class AppKbController {

    /** 文档状态：已发布（0=草稿，用户端不可见） */
    private static final String STATUS_PUBLISHED = "1";

    /** 管理端知识文档列表权限：未发布文档仅持此权限的管理员可见 */
    private static final String KB_LIST_PERMISSION = "kb:document:list";

    private final IKbDocumentService documentService;

    private final IKbCategoryService categoryService;

    private final PermissionService permissionService;

    /**
     * 知识分类列表（仅启用状态，按排序号升序）
     * GET /api/app/v1/kb/categories
     */
    @Operation(summary = "知识分类列表")
    @GetMapping("/categories")
    public Result<List<KbCategory>> categories() {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        List<KbCategory> categories = categoryService.list(new LambdaQueryWrapper<KbCategory>()
                .eq(KbCategory::getStatus, 1)
                .orderByAsc(KbCategory::getSort)
                .orderByAsc(KbCategory::getId));
        return Result.success(categories);
    }

    /**
     * 已发布文档分页列表（分类筛选 + 标题关键字）
     * GET /api/app/v1/kb/documents
     *
     * <p>状态过滤服务端强制注入（仅 status=1 已发布），接口不接收 status 参数；
     * 分页与筛选参数沿用管理端同名列查询口径。</p>
     */
    @Operation(summary = "已发布文档分页列表")
    @GetMapping("/documents")
    public Result<PageResult<KbDocument>> documents(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String title) {

        PageParamGuard.validate(page, pageSize);

        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        LambdaQueryWrapper<KbDocument> wrapper = new LambdaQueryWrapper<KbDocument>()
                .eq(KbDocument::getStatus, STATUS_PUBLISHED);
        if (categoryId != null) {
            wrapper.eq(KbDocument::getCategoryId, categoryId);
        }
        if (title != null && !title.isEmpty()) {
            wrapper.like(KbDocument::getTitle, title);
        }
        wrapper.orderByDesc(KbDocument::getCreateTime).orderByDesc(KbDocument::getId);
        IPage<KbDocument> result = documentService.page(new Page<>(page, pageSize), wrapper);
        return Result.page(result);
    }

    /**
     * 文档详情（查看时浏览量原子 +1）
     * GET /api/app/v1/kb/documents/{id}
     *
     * <p>状态可见性校验：未发布（草稿）文档仅持管理端知识文档权限的管理员可见，
     * 普通员工返回错误且不计浏览量。</p>
     */
    @Operation(summary = "文档详情")
    @GetMapping("/documents/{id}")
    public Result<KbDocument> detail(@PathVariable Long id) {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        KbDocument doc = documentService.getById(id);
        if (doc == null) {
            return Result.error("文档不存在");
        }
        // 状态可见性：草稿仅管理员可见；普通员工不可见也不计浏览量
        boolean admin = permissionService.hasPermission(userId, KB_LIST_PERMISSION);
        if (!STATUS_PUBLISHED.equals(doc.getStatus()) && !admin) {
            return Result.error("文档未发布或已下线");
        }
        // 复用管理端浏览量原子自增（setSql view_count = view_count + 1，并发安全）
        return Result.success(documentService.viewDocument(id));
    }
}

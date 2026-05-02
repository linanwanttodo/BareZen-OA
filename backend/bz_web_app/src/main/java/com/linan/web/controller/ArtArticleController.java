package com.linan.web.controller;

import com.linan.article.entity.ArtArticle;
import com.linan.article.entity.ArtCategory;
import com.linan.article.service.IArtArticleService;
import com.linan.article.service.IArtCategoryService;
import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.common.controller.BaseController;
import com.linan.core.security.RequirePermission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 文章内容管理 Controller
 */
@Tag(name = "文章管理", description = "文章内容 CRUD")
@RestController
@RequestMapping("/api/system/v1/articles")
public class ArtArticleController extends BaseController<ArtArticle, IArtArticleService> {

    @Autowired
    private IArtArticleService articleService;

    @Autowired
    private IArtCategoryService categoryService;

    @Override
    protected IArtArticleService getService() {
        return articleService;
    }

    @RequirePermission("system:article:list")
    @Operation(summary = "分页查询文章")
    @GetMapping("/page")
    @Override
    public Result<PageResult<ArtArticle>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return super.list(pageNum, pageSize);
    }

    @RequirePermission("system:article:list")
    @Operation(summary = "查询文章详情")
    @GetMapping("/{id}")
    @Override
    public Result<ArtArticle> getById(@PathVariable Long id) {
        return super.getById(id);
    }

    @RequirePermission("system:article:add")
    @Operation(summary = "新增文章")
    @PostMapping
    @Override
    public Result<Void> save(@jakarta.validation.Valid @RequestBody ArtArticle entity) {
        return super.save(entity);
    }

    @RequirePermission("system:article:edit")
    @Operation(summary = "更新文章")
    @PutMapping
    @Override
    public Result<Void> update(@jakarta.validation.Valid @RequestBody ArtArticle entity) {
        return super.update(entity);
    }

    @RequirePermission("system:article:delete")
    @Operation(summary = "删除文章")
    @DeleteMapping("/{id}")
    @Override
    public Result<Void> delete(@PathVariable Long id) {
        return super.delete(id);
    }

    /**
     * 获取所有分类（供下拉选择）
     */
    @RequirePermission("system:article:list")
    @Operation(summary = "获取所有文章分类")
    @GetMapping("/categories")
    public Result<List<ArtCategory>> categories() {
        return Result.success(categoryService.listAll());
    }
}

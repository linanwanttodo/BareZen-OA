package com.linan.web.controller;

import com.linan.article.entity.ArtArticle;
import com.linan.article.entity.ArtCategory;
import com.linan.article.service.IArtArticleService;
import com.linan.article.service.IArtCategoryService;
import com.linan.core.common.api.Result;
import com.linan.core.common.controller.BaseController;
import com.linan.core.security.RequirePermission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 文章内容管理 Controller
 */
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

    /**
     * 获取所有分类（供下拉选择）
     */
    @RequirePermission("system:article:list")
    @GetMapping("/categories")
    public Result<List<ArtCategory>> categories() {
        return Result.success(categoryService.listAll());
    }
}

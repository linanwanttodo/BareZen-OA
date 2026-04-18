package com.linan.article.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.linan.article.entity.ArtArticle;

public interface IArtArticleService extends IService<ArtArticle> {

    Page<ArtArticle> page(Page<ArtArticle> page, String keyword, Long categoryId, String tag);

    ArtArticle getByIdWithAuthor(Long id);

    void incrementViewCount(Long id);

    void incrementLikeCount(Long id);
}

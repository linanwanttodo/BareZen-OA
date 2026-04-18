package com.linan.article.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.linan.article.entity.ArtComment;

public interface IArtCommentService extends IService<ArtComment> {

    Page<ArtComment> pageByArticle(Long articleId, Page<ArtComment> page);
}

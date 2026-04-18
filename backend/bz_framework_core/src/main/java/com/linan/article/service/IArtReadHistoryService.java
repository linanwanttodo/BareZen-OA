package com.linan.article.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.linan.article.entity.ArtArticle;
import com.linan.article.entity.ArtReadHistory;

public interface IArtReadHistoryService extends IService<ArtReadHistory> {

    /** 记录阅读历史（已存在则更新时间） */
    void record(Long userId, Long articleId, String articleTitle, String articleSummary, String authorName);

    /** 阅读历史列表（分页，按时间倒序） */
    Page<ArtArticle> pageHistory(Long userId, Page<ArtArticle> page);

    /** 清空阅读历史 */
    void clear(Long userId);
}

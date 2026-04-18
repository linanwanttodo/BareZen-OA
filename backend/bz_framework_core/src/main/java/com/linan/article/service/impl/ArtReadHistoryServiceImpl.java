package com.linan.article.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.article.entity.ArtArticle;
import com.linan.article.entity.ArtReadHistory;
import com.linan.article.mapper.ArtArticleMapper;
import com.linan.article.mapper.ArtReadHistoryMapper;
import com.linan.article.service.IArtReadHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ArtReadHistoryServiceImpl extends ServiceImpl<ArtReadHistoryMapper, ArtReadHistory> implements IArtReadHistoryService {

    @Autowired
    private ArtArticleMapper articleMapper;

    @Override
    public void record(Long userId, Long articleId, String articleTitle, String articleSummary, String authorName) {
        // 查是否已存在
        LambdaQueryWrapper<ArtReadHistory> qw = new LambdaQueryWrapper<>();
        qw.eq(ArtReadHistory::getUserId, userId)
          .eq(ArtReadHistory::getArticleId, articleId);

        ArtReadHistory existing = getOne(qw);
        if (existing != null) {
            // 已存在则更新时间和标题（可能文章被编辑过）
            existing.setArticleTitle(articleTitle);
            existing.setArticleSummary(articleSummary);
            existing.setAuthorName(authorName);
            baseMapper.updateById(existing);
        } else {
            ArtReadHistory history = new ArtReadHistory();
            history.setUserId(userId);
            history.setArticleId(articleId);
            history.setArticleTitle(articleTitle);
            history.setArticleSummary(articleSummary);
            history.setAuthorName(authorName);
            save(history);
        }
    }

    @Override
    public Page<ArtArticle> pageHistory(Long userId, Page<ArtArticle> page) {
        // 先查历史记录（按时间倒序）
        LambdaQueryWrapper<ArtReadHistory> historyQw = new LambdaQueryWrapper<>();
        historyQw.eq(ArtReadHistory::getUserId, userId)
                  .orderByDesc(ArtReadHistory::getCreateTime);

        Page<ArtReadHistory> historyPage = new Page<>(page.getCurrent(), page.getSize());
        Page<ArtReadHistory> historyResult = baseMapper.selectPage(historyPage, historyQw);

        // 转换为文章分页
        Page<ArtArticle> result = new Page<>(page.getCurrent(), page.getSize(), historyResult.getTotal());

        List<Long> articleIds = historyResult.getRecords().stream()
                .map(ArtReadHistory::getArticleId)
                .collect(Collectors.toList());

        if (articleIds.isEmpty()) {
            result.setRecords(List.of());
            return result;
        }

        // 批量查询文章
        List<ArtArticle> articles = articleMapper.selectBatchIds(articleIds);

        // 保持历史记录的顺序
        List<ArtArticle> ordered = articleIds.stream()
                .map(id -> articles.stream().filter(a -> a.getId().equals(id)).findFirst().orElse(null))
                .filter(a -> a != null)
                .collect(Collectors.toList());

        result.setRecords(ordered);
        return result;
    }

    @Override
    public void clear(Long userId) {
        LambdaQueryWrapper<ArtReadHistory> qw = new LambdaQueryWrapper<>();
        qw.eq(ArtReadHistory::getUserId, userId);
        remove(qw);
    }
}

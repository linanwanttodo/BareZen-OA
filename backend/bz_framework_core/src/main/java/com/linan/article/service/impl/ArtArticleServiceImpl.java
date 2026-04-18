package com.linan.article.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.article.entity.ArtArticle;
import com.linan.article.mapper.ArtArticleMapper;
import com.linan.article.service.IArtArticleService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ArtArticleServiceImpl extends ServiceImpl<ArtArticleMapper, ArtArticle> implements IArtArticleService {

    @Override
    public Page<ArtArticle> page(Page<ArtArticle> page, String keyword, Long categoryId, String tag) {
        LambdaQueryWrapper<ArtArticle> qw = new LambdaQueryWrapper<>();
        qw.eq(ArtArticle::getStatus, 1);
        qw.orderByDesc(ArtArticle::getCreateTime);

        if (categoryId != null) {
            qw.eq(ArtArticle::getCategoryId, categoryId);
        }

        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(ArtArticle::getTitle, keyword)
                    .or().like(ArtArticle::getSummary, keyword));
        }

        if (StringUtils.hasText(tag)) {
            qw.like(ArtArticle::getTags, tag);
        }

        return baseMapper.selectPage(page, qw);
    }

    @Override
    public ArtArticle getByIdWithAuthor(Long id) {
        ArtArticle article = baseMapper.selectById(id);
        if (article != null) {
            article.setViewCount(article.getViewCount() == null ? 0 : article.getViewCount());
            article.setLikeCount(article.getLikeCount() == null ? 0 : article.getLikeCount());
            article.setCommentCount(article.getCommentCount() == null ? 0 : article.getCommentCount());
        }
        return article;
    }

    @Override
    public void incrementViewCount(Long id) {
        baseMapper.update(
                new ArtArticle(),
                new UpdateWrapper<ArtArticle>()
                        .setSql("view_count = COALESCE(view_count, 0) + 1")
                        .eq("id", id)
        );
    }

    @Override
    public void incrementLikeCount(Long id) {
        baseMapper.update(
                new ArtArticle(),
                new UpdateWrapper<ArtArticle>()
                        .setSql("like_count = COALESCE(like_count, 0) + 1")
                        .eq("id", id)
        );
    }
}

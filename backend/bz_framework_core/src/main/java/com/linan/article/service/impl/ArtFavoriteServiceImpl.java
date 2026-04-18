package com.linan.article.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.article.entity.ArtArticle;
import com.linan.article.entity.ArtFavorite;
import com.linan.article.mapper.ArtArticleMapper;
import com.linan.article.mapper.ArtFavoriteMapper;
import com.linan.article.service.IArtFavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ArtFavoriteServiceImpl extends ServiceImpl<ArtFavoriteMapper, ArtFavorite> implements IArtFavoriteService {

    @Autowired
    private ArtArticleMapper articleMapper;

    @Override
    public boolean toggle(Long userId, Long articleId) {
        LambdaQueryWrapper<ArtFavorite> qw = new LambdaQueryWrapper<>();
        qw.eq(ArtFavorite::getUserId, userId)
          .eq(ArtFavorite::getArticleId, articleId);

        ArtFavorite existing = getOne(qw);
        if (existing != null) {
            removeById(existing.getId());
            return false; // 已取消
        } else {
            ArtFavorite fav = new ArtFavorite();
            fav.setUserId(userId);
            fav.setArticleId(articleId);
            save(fav);
            return true; // 已收藏
        }
    }

    @Override
    public boolean isFavorited(Long userId, Long articleId) {
        LambdaQueryWrapper<ArtFavorite> qw = new LambdaQueryWrapper<>();
        qw.eq(ArtFavorite::getUserId, userId)
          .eq(ArtFavorite::getArticleId, articleId);
        return count(qw) > 0;
    }

    @Override
    public Page<ArtArticle> pageFavorites(Long userId, Page<ArtArticle> page) {
        // 先查收藏记录（按时间倒序）
        LambdaQueryWrapper<ArtFavorite> favQw = new LambdaQueryWrapper<>();
        favQw.eq(ArtFavorite::getUserId, userId)
              .orderByDesc(ArtFavorite::getCreateTime);

        Page<ArtFavorite> favPage = new Page<>(page.getCurrent(), page.getSize());
        Page<ArtFavorite> favResult = baseMapper.selectPage(favPage, favQw);

        // 转换为文章分页
        Page<ArtArticle> result = new Page<>(page.getCurrent(), page.getSize(), favResult.getTotal());

        List<Long> articleIds = favResult.getRecords().stream()
                .map(ArtFavorite::getArticleId)
                .collect(Collectors.toList());

        if (articleIds.isEmpty()) {
            result.setRecords(List.of());
            return result;
        }

        // 批量查询文章
        List<ArtArticle> articles = articleMapper.selectBatchIds(articleIds);

        // 保持收藏的顺序
        List<ArtArticle> ordered = articleIds.stream()
                .map(id -> articles.stream().filter(a -> a.getId().equals(id)).findFirst().orElse(null))
                .filter(a -> a != null)
                .collect(Collectors.toList());

        result.setRecords(ordered);
        return result;
    }
}

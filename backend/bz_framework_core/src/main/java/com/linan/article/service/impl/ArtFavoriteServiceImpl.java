package com.linan.article.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.article.entity.ArtArticle;
import com.linan.article.entity.ArtFavorite;
import com.linan.article.mapper.ArtArticleMapper;
import com.linan.article.mapper.ArtFavoriteMapper;
import com.linan.article.service.IArtFavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ArtFavoriteServiceImpl extends ServiceImpl<ArtFavoriteMapper, ArtFavorite> implements IArtFavoriteService {

    @Autowired
    private ArtArticleMapper articleMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
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
        // 单次 JOIN 查询，按收藏时间倒序，替代原来的 N+1 方案
        IPage<ArtArticle> result = articleMapper.selectFavoritedArticlesByUserId(page, userId);
        return (Page<ArtArticle>) result;
    }
}

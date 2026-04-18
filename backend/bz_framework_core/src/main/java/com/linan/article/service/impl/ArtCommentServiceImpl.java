package com.linan.article.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.article.entity.ArtComment;
import com.linan.article.mapper.ArtCommentMapper;
import com.linan.article.service.IArtCommentService;
import org.springframework.stereotype.Service;

@Service
public class ArtCommentServiceImpl extends ServiceImpl<ArtCommentMapper, ArtComment> implements IArtCommentService {

    @Override
    public Page<ArtComment> pageByArticle(Long articleId, Page<ArtComment> page) {
        LambdaQueryWrapper<ArtComment> qw = new LambdaQueryWrapper<>();
        qw.eq(ArtComment::getArticleId, articleId)
          .orderByAsc(ArtComment::getCreateTime);
        return baseMapper.selectPage(page, qw);
    }
}

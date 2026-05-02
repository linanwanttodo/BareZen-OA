package com.linan.article.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.article.entity.ArtArticle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ArtArticleMapper extends BaseMapper<ArtArticle> {

    /**
     * 查询用户收藏的文章列表（JOIN 优化，避免 N+1）
     *
     * @param page   分页参数
     * @param userId 用户ID
     * @return 分页文章列表（按收藏时间倒序）
     */
    IPage<ArtArticle> selectFavoritedArticlesByUserId(Page<?> page, @Param("userId") Long userId);
}

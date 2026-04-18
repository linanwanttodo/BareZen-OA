package com.linan.article.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.linan.article.entity.ArtArticle;
import com.linan.article.entity.ArtFavorite;

public interface IArtFavoriteService extends IService<ArtFavorite> {

    /** 收藏/取消收藏，返回 true=已收藏, false=已取消 */
    boolean toggle(Long userId, Long articleId);

    /** 是否已收藏 */
    boolean isFavorited(Long userId, Long articleId);

    /** 收藏列表（分页，含文章信息） */
    Page<ArtArticle> pageFavorites(Long userId, Page<ArtArticle> page);
}

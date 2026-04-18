package com.linan.article.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("art_article")
public class ArtArticle {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long authorId;

    private String authorName;

    private Long categoryId;

    private String categoryName;

    private String title;

    private String summary;

    private String content;

    private String coverImage;

    private Integer viewCount;

    private Integer likeCount;

    private Integer commentCount;

    private Integer status;

    private String tags;

    private Integer orderNum;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

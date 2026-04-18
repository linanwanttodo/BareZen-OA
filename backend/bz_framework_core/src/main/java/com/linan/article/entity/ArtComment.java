package com.linan.article.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("art_comment")
public class ArtComment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long articleId;

    private Long userId;

    private String userName;

    private String userAvatar;

    private String content;

    private Long parentId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}

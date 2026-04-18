package com.linan.article.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("art_read_history")
public class ArtReadHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long articleId;

    private String articleTitle;

    private String articleSummary;

    private String authorName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}

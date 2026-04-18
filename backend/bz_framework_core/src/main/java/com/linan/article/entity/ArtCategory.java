package com.linan.article.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("art_category")
public class ArtCategory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    private Integer orderNum;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}

package com.linan.kb.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.linan.core.common.entity.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识文档实体，对应 kb_document 表
 *
 * @author BareZen Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kb_document")
public class KbDocument extends BaseEntity {

    /** 文档标题 */
    @NotBlank(message = "文档标题不能为空")
    private String title;

    /** 分类：general=通用 manual=制度手册 process=流程规范 tech=技术资料 other=其他 */
    private String category;

    /** 知识分类ID（V41：关联 kb_category，替代枚举字符串） */
    private Long categoryId;

    /** 文档正文（HTML/富文本） */
    private String content;

    /** 所属部门ID（null=全公司可见） */
    private Long deptId;

    /** 所属部门名称（冗余） */
    private String deptName;

    /** 作者用户ID */
    private Long authorId;

    /** 作者姓名 */
    private String authorName;

    /** 状态：0=草稿 1=已发布 */
    private String status;

    /** 浏览量 */
    private Integer viewCount;

    /** 备注 */
    private String remark;
}

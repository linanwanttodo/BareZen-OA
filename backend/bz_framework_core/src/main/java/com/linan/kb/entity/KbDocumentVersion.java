package com.linan.kb.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.linan.core.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识文档版本实体，对应 kb_document_version 表
 *
 * <p>V41 新增：保存文档每次更新前的完整副本（内部 OA 数据量小，不存差异），
 * 首版创建时即生成 v1，保证任意历史版本可回溯。</p>
 *
 * @author BareZen Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kb_document_version")
public class KbDocumentVersion extends BaseEntity {

    /** 文档ID */
    private Long docId;

    /** 版本号（从1递增） */
    private Integer versionNo;

    /** 版本标题 */
    private String title;

    /** 版本分类ID */
    private Long categoryId;

    /** 版本正文（完整副本） */
    private String content;

    /** 操作者用户ID */
    private Long operatorId;

    /** 操作者姓名 */
    private String operatorName;
}
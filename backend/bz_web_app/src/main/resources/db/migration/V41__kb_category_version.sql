-- ============================================
-- BareZen_Base V41: 知识库分类 + 文档版本（2.3）
-- ============================================
-- 背景：V13 kb_document.category 为硬编码枚举字符串，content 原地更新无版本留痕。
--       本迁移：
--         1) kb_category 分类表（种子映射现有 5 枚举，支持树形扩展）
--         2) kb_document 加 category_id 列，按枚举映射回填
--         3) kb_document_version 版本表（存完整副本，内部 OA 数据量小不存差异）
-- 约定：kb_document.category 列保留（兼容旧查询），新业务按 category_id 关联；
--       PUT 更新文档前先写版本快照（首版也生成 v1），保证任意历史版本可回溯。
-- 说明：原计划 V39 已被资产分类+借用表占用，本迁移顺延 V41。

-- 1. 知识库分类表
CREATE TABLE IF NOT EXISTS `kb_category` (
  `id` BIGINT NOT NULL COMMENT '主键ID（雪花ID）',
  `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
  `parent_id` BIGINT DEFAULT 0 COMMENT '父分类ID（0=根分类，支持树形）',
  `sort` INT DEFAULT 0 COMMENT '排序号',
  `status` TINYINT DEFAULT 1 COMMENT '是否启用：1=启用 0=停用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建者ID',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新者ID',
  `del_flag` TINYINT DEFAULT 0 COMMENT '逻辑删除：0=未删 1=已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`, `del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库分类表';

-- 2. 知识文档加分类ID列
ALTER TABLE `kb_document`
  ADD COLUMN `category_id` BIGINT DEFAULT NULL COMMENT '知识分类ID（关联 kb_category）' AFTER `category`;

-- 3. 分类种子：映射 V13 现有 5 枚举
INSERT IGNORE INTO `kb_category` (`id`, `name`, `parent_id`, `sort`, `status`) VALUES
(1, '通用', 0, 1, 1),
(2, '制度手册', 0, 2, 1),
(3, '流程规范', 0, 3, 1),
(4, '技术资料', 0, 4, 1),
(5, '其他', 0, 5, 1);

-- 4. 按枚举映射回填 category_id
UPDATE `kb_document` SET `category_id` = 1 WHERE `category` = 'general' AND `category_id` IS NULL;
UPDATE `kb_document` SET `category_id` = 2 WHERE `category` = 'manual' AND `category_id` IS NULL;
UPDATE `kb_document` SET `category_id` = 3 WHERE `category` = 'process' AND `category_id` IS NULL;
UPDATE `kb_document` SET `category_id` = 4 WHERE `category` = 'tech' AND `category_id` IS NULL;
UPDATE `kb_document` SET `category_id` = 5 WHERE `category` = 'other' AND `category_id` IS NULL;
UPDATE `kb_document` SET `category_id` = 5 WHERE `category_id` IS NULL;

-- 5. 文档版本表
CREATE TABLE IF NOT EXISTS `kb_document_version` (
  `id` BIGINT NOT NULL COMMENT '主键ID（雪花ID）',
  `doc_id` BIGINT NOT NULL COMMENT '文档ID',
  `version_no` INT NOT NULL COMMENT '版本号（从1递增）',
  `title` VARCHAR(200) NOT NULL COMMENT '版本标题',
  `category_id` BIGINT DEFAULT NULL COMMENT '版本分类ID',
  `content` LONGTEXT COMMENT '版本正文（完整副本）',
  `operator_id` BIGINT DEFAULT NULL COMMENT '操作者用户ID',
  `operator_name` VARCHAR(50) DEFAULT NULL COMMENT '操作者姓名',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '版本创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建者ID',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新者ID',
  `del_flag` TINYINT DEFAULT 0 COMMENT '逻辑删除：0=未删 1=已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_doc_version` (`doc_id`, `version_no`, `del_flag`),
  KEY `idx_doc` (`doc_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识文档版本表';
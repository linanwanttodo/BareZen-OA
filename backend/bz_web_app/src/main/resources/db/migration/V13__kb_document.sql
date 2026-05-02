-- ============================================
-- BareZen_Base V13: 知识库/文档库模块
-- ============================================
-- 公司内部管理系统：部门知识沉淀，支持分类、正文（富文本/纯文本）、发布状态。

-- 1. 知识文档表
CREATE TABLE IF NOT EXISTS `kb_document` (
  `id` BIGINT NOT NULL COMMENT '主键ID（雪花ID）',
  `title` VARCHAR(200) NOT NULL COMMENT '文档标题',
  `category` VARCHAR(50) DEFAULT 'general' COMMENT '分类：general=通用 manual=制度手册 process=流程规范 tech=技术资料 other=其他',
  `content` LONGTEXT COMMENT '文档正文（HTML/富文本）',
  `dept_id` BIGINT DEFAULT NULL COMMENT '所属部门ID（null=全公司可见）',
  `dept_name` VARCHAR(100) DEFAULT NULL COMMENT '所属部门名称（冗余）',
  `author_id` BIGINT DEFAULT NULL COMMENT '作者用户ID',
  `author_name` VARCHAR(50) DEFAULT NULL COMMENT '作者姓名',
  `status` VARCHAR(4) NOT NULL DEFAULT '0' COMMENT '状态：0=草稿 1=已发布',
  `view_count` INT DEFAULT 0 COMMENT '浏览量',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建者ID',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新者ID',
  `del_flag` TINYINT DEFAULT 0 COMMENT '逻辑删除：0=未删 1=已删',
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category`),
  KEY `idx_status` (`status`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识文档表';

-- 2. 知识库菜单（id=22 目录, id=23 知识文档菜单，挂载在系统管理或独立目录）
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `perms`, `path`, `component`, `icon`, `order_num`, `visible`, `status`, `remark`) VALUES
(22, 0, '知识库', 'M', NULL, 'kb', NULL, 'book', 8, '0', '1', '知识库目录'),
(23, 22, '知识文档', 'C', 'kb:document:list', 'document', 'kb/document/index', 'file', 1, '0', '1', '知识文档菜单');

-- 3. admin 角色授予知识库权限
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (1, 22), (1, 23);

-- 4. 示例知识文档
INSERT IGNORE INTO `kb_document` (`id`, `title`, `category`, `content`, `dept_id`, `dept_name`, `author_id`, `author_name`, `status`, `view_count`, `create_time`, `update_time`, `create_by`, `update_by`, `del_flag`)
VALUES (1, 'BareZen 系统使用指南', 'manual', '<p>欢迎使用 BareZen 公司内部管理系统。本指南介绍系统各模块的基本用法。</p><h3>快速上手</h3><ol><li>使用账号密码登录系统</li><li>在左侧菜单进入对应功能模块</li><li>人事管理：维护员工档案与请假审批</li><li>资产管理：登记公司固定资产</li></ol>', NULL, NULL, 1, '超级管理员', '1', 0, NOW(), NOW(), 1, 1, 0);

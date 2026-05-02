-- ============================================
-- BareZen_Base V2: 业务表结构
-- ============================================

-- 1. 通知公告表
CREATE TABLE IF NOT EXISTS `sys_notice` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `notice_title` varchar(128) NOT NULL COMMENT '通知标题',
  `notice_content` text COMMENT '通知内容（HTML富文本）',
  `notice_type` char(1) DEFAULT '2' COMMENT '类型：1=通知（弹窗） 2=公告（首页展示）',
  `status` char(1) DEFAULT '0' COMMENT '状态：0=草稿 1=已发布 2=已下架',
  `scope` varchar(10) DEFAULT 'public' COMMENT '可见范围：public=全部用户 internal=仅站内用户',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建者ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新者ID',
  `del_flag` tinyint DEFAULT 0 COMMENT '逻辑删除（0=正常 1=已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_type_status` (`notice_type`, `status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知公告表';

-- 2. 字典类型表
CREATE TABLE IF NOT EXISTS `sys_dict_type` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `dict_name` varchar(100) NOT NULL COMMENT '字典名称',
  `dict_type` varchar(100) NOT NULL COMMENT '字典类型（唯一标识）',
  `status` char(1) DEFAULT '1' COMMENT '状态（0=停用 1=正常）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建者ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新者ID',
  `del_flag` tinyint DEFAULT 0 COMMENT '逻辑删除',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dict_type` (`dict_type`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典类型表';

-- 3. 字典数据表
CREATE TABLE IF NOT EXISTS `sys_dict_data` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `dict_sort` int DEFAULT 0 COMMENT '字典排序',
  `dict_label` varchar(100) NOT NULL COMMENT '字典标签',
  `dict_value` varchar(100) NOT NULL COMMENT '字典键值',
  `dict_type` varchar(100) NOT NULL COMMENT '字典类型',
  `css_class` varchar(100) DEFAULT '' COMMENT '样式属性',
  `list_class` varchar(100) DEFAULT '' COMMENT '表格回显样式',
  `is_default` char(1) DEFAULT 'N' COMMENT '是否默认（Y=是 N=否）',
  `status` char(1) DEFAULT '1' COMMENT '状态（0=停用 1=正常）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建者ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新者ID',
  `del_flag` tinyint DEFAULT 0 COMMENT '逻辑删除',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_dict_type` (`dict_type`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典数据表';

-- 4. 操作日志表
CREATE TABLE IF NOT EXISTS `sys_oper_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` varchar(50) DEFAULT '' COMMENT '模块标题',
  `business_type` int DEFAULT '0' COMMENT '业务类型',
  `method` varchar(100) DEFAULT '' COMMENT '方法名称',
  `request_method` varchar(10) DEFAULT '' COMMENT '请求方式',
  `oper_user` varchar(50) DEFAULT '' COMMENT '操作人员',
  `oper_ip` varchar(128) DEFAULT '' COMMENT '主机地址',
  `oper_url` varchar(255) DEFAULT '' COMMENT '请求URL',
  `class_name` varchar(255) DEFAULT '' COMMENT '类名称',
  `method_name` varchar(100) DEFAULT '' COMMENT '方法名称',
  `oper_param` varchar(2000) DEFAULT '' COMMENT '请求参数',
  `json_result` varchar(2000) DEFAULT '' COMMENT '返回参数',
  `status` int DEFAULT '0' COMMENT '操作状态（0正常 1异常）',
  `error_msg` varchar(2000) DEFAULT '' COMMENT '错误消息',
  `cost_time` bigint DEFAULT '0' COMMENT '消耗时间（毫秒）',
  `oper_time` datetime DEFAULT NULL COMMENT '操作时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建者ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新者ID',
  `del_flag` tinyint DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_oper_user` (`oper_user`),
  KEY `idx_oper_time` (`oper_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志记录';

-- 5. 文件管理表
CREATE TABLE IF NOT EXISTS `sys_file` (
  `id` bigint NOT NULL COMMENT '主键ID（雪花算法）',
  `file_name` varchar(100) DEFAULT '' COMMENT '文件存储名',
  `original_name` varchar(255) DEFAULT '' COMMENT '原始文件名',
  `file_path` varchar(500) DEFAULT '' COMMENT '文件存储路径',
  `file_url` varchar(500) DEFAULT '' COMMENT '访问URL',
  `file_size` bigint DEFAULT '0' COMMENT '文件大小（字节）',
  `file_type` varchar(50) DEFAULT '' COMMENT '文件类型',
  `storage_type` varchar(20) DEFAULT 'local' COMMENT '存储类型（local/oss）',
  `is_folder` tinyint(1) DEFAULT '0' COMMENT '是否文件夹（0=文件，1=文件夹）',
  `folder_id` bigint DEFAULT '0' COMMENT '父文件夹ID（0=根目录）',
  `scope` varchar(10) DEFAULT 'admin' COMMENT '可见范围（admin/public）',
  `sort_order` int DEFAULT '0' COMMENT '排序号',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `del_flag` int DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  KEY `idx_folder_id` (`folder_id`),
  KEY `idx_scope` (`scope`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件管理记录';

-- 6. 文章分类表
CREATE TABLE IF NOT EXISTS `art_category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '分类描述',
  `order_num` INT DEFAULT 0 COMMENT '显示顺序',
  `status` TINYINT DEFAULT 1 COMMENT '状态 (0=停用, 1=正常)',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章分类表';

-- 7. 文章表
CREATE TABLE IF NOT EXISTS `art_article` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '文章ID',
  `author_id` BIGINT NOT NULL COMMENT '作者用户ID',
  `author_name` VARCHAR(50) DEFAULT NULL COMMENT '作者昵称',
  `category_id` BIGINT DEFAULT NULL COMMENT '分类ID',
  `category_name` VARCHAR(50) DEFAULT NULL COMMENT '分类名称',
  `title` VARCHAR(200) NOT NULL COMMENT '文章标题',
  `summary` VARCHAR(500) DEFAULT NULL COMMENT '文章摘要',
  `content` LONGTEXT NOT NULL COMMENT '文章正文 (HTML)',
  `cover_image` VARCHAR(500) DEFAULT NULL COMMENT '封面图URL',
  `view_count` INT DEFAULT 0 COMMENT '浏览量',
  `like_count` INT DEFAULT 0 COMMENT '点赞数',
  `comment_count` INT DEFAULT 0 COMMENT '评论数',
  `status` TINYINT DEFAULT 1 COMMENT '状态 (0=草稿, 1=已发布)',
  `tags` VARCHAR(255) DEFAULT NULL COMMENT '标签 (逗号分隔)',
  `order_num` INT DEFAULT 0 COMMENT '排序',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_author_id` (`author_id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章表';

-- 8. 评论表
CREATE TABLE IF NOT EXISTS `art_comment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `article_id` BIGINT NOT NULL COMMENT '文章ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `user_name` VARCHAR(50) DEFAULT NULL COMMENT '用户名',
  `user_avatar` VARCHAR(500) DEFAULT NULL COMMENT '用户头像',
  `content` VARCHAR(1000) NOT NULL COMMENT '评论内容',
  `parent_id` BIGINT DEFAULT NULL COMMENT '父评论ID (回复)',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_article_id` (`article_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论表';

-- 9. 收藏表
CREATE TABLE IF NOT EXISTS `art_favorite` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `article_id` BIGINT NOT NULL COMMENT '文章ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_article` (`user_id`, `article_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章收藏表';

-- 10. 阅读历史表
CREATE TABLE IF NOT EXISTS `art_read_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `article_id` BIGINT NOT NULL COMMENT '文章ID',
  `article_title` VARCHAR(200) DEFAULT NULL COMMENT '文章标题（冗余）',
  `article_summary` VARCHAR(500) DEFAULT NULL COMMENT '文章摘要（冗余）',
  `author_name` VARCHAR(50) DEFAULT NULL COMMENT '作者名（冗余）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '阅读时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_article` (`user_id`, `article_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='阅读历史表';

-- 11. 待办事项表
CREATE TABLE IF NOT EXISTS `todo_item` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `title` VARCHAR(200) NOT NULL COMMENT '待办标题',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '待办描述',
  `user_id` BIGINT NOT NULL COMMENT '所属用户ID',
  `priority` TINYINT NOT NULL DEFAULT 1 COMMENT '优先级：0=低 1=中 2=高 3=紧急',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0=待办 1=进行中 2=已完成 3=已取消',
  `due_date` VARCHAR(20) DEFAULT NULL COMMENT '截止日期',
  `completed_time` VARCHAR(30) DEFAULT NULL COMMENT '完成时间',
  `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建者',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新者',
  `del_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记：0=未删除 1=已删除',
  PRIMARY KEY (`id`),
  KEY `idx_todo_user` (`user_id`),
  KEY `idx_todo_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='待办事项表';

-- 12. 系统配置表
CREATE TABLE IF NOT EXISTS `sys_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '参数主键',
  `config_name` varchar(100) DEFAULT '' COMMENT '参数名称',
  `config_key` varchar(100) DEFAULT '' COMMENT '参数键名',
  `config_value` varchar(500) DEFAULT '' COMMENT '参数键值',
  `config_type` char(1) DEFAULT 'Y' COMMENT '系统内置（Y=是 N=否）',
  `status` char(1) DEFAULT '1' COMMENT '状态（0=停用 1=正常）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `del_flag` tinyint DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

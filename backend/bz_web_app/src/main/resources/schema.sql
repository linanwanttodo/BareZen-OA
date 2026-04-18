-- 企业用户端扩展表
-- 待办事项表（应用启动时自动执行，IF NOT EXISTS 保证幂等）
CREATE TABLE IF NOT EXISTS `todo_item` (
    `id`             BIGINT       NOT NULL COMMENT '主键ID',
    `title`          VARCHAR(200) NOT NULL COMMENT '待办标题',
    `description`    VARCHAR(500) DEFAULT NULL COMMENT '待办描述',
    `user_id`        BIGINT       NOT NULL COMMENT '所属用户ID',
    `priority`       TINYINT      NOT NULL DEFAULT 1 COMMENT '优先级：0=低 1=中 2=高 3=紧急',
    `status`         TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0=待办 1=进行中 2=已完成 3=已取消',
    `due_date`       VARCHAR(20)  DEFAULT NULL COMMENT '截止日期',
    `completed_time` VARCHAR(30)  DEFAULT NULL COMMENT '完成时间',
    `create_time`    DATETIME     DEFAULT NULL COMMENT '创建时间',
    `update_time`    DATETIME     DEFAULT NULL COMMENT '更新时间',
    `create_by`      BIGINT       DEFAULT NULL COMMENT '创建者',
    `update_by`      BIGINT       DEFAULT NULL COMMENT '更新者',
    `del_flag`       TINYINT      NOT NULL DEFAULT 0 COMMENT '删除标记：0=未删除 1=已删除',
    PRIMARY KEY (`id`),
    KEY `idx_todo_user` (`user_id`),
    KEY `idx_todo_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='待办事项表';

-- ============================================
-- 部门和用户-部门关联表
-- ============================================

-- 部门表
CREATE TABLE IF NOT EXISTS `sys_dept` (
  `id`          BIGINT       NOT NULL COMMENT '部门ID',
  `parent_id`   BIGINT       DEFAULT 0 COMMENT '父部门ID（0=顶级）',
  `dept_name`   VARCHAR(100) NOT NULL COMMENT '部门名称',
  `sort`        INT          DEFAULT 0 COMMENT '显示顺序',
  `leader`      VARCHAR(50)  DEFAULT NULL COMMENT '负责人',
  `phone`       VARCHAR(20)  DEFAULT NULL COMMENT '联系电话',
  `email`       VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `status`      CHAR(1)      DEFAULT '0' COMMENT '部门状态（0=正常 1=停用）',
  `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by`   BIGINT       DEFAULT NULL COMMENT '创建者ID',
  `update_by`   BIGINT       DEFAULT NULL COMMENT '更新者ID',
  `del_flag`    TINYINT      DEFAULT 0 COMMENT '逻辑删除（0=正常 1=已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- 用户-部门关联表（含部门内角色类型）
CREATE TABLE IF NOT EXISTS `sys_user_dept` (
  `user_id`   BIGINT  NOT NULL COMMENT '用户ID',
  `dept_id`   BIGINT  NOT NULL COMMENT '部门ID',
  `role_type` CHAR(1) NOT NULL DEFAULT '0' COMMENT '部门内角色（0=普通用户 1=管理员）',
  PRIMARY KEY (`user_id`, `dept_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_dept_id` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户部门关联表';

-- 初始化部门数据
INSERT IGNORE INTO `sys_dept` VALUES (1, 0, '总公司', 1, '超级管理员', '13888888888', 'admin@barezen.com', '0', NOW(), NOW(), NULL, NULL, 0);

-- 关联 admin 用户到总公司，角色为管理员
INSERT IGNORE INTO `sys_user_dept` VALUES (1, 1, '1');

-- ============================================
-- 文件管理表（sys_file）
-- ============================================
CREATE TABLE IF NOT EXISTS `sys_file` (
  `id`            BIGINT       NOT NULL COMMENT '主键ID（雪花算法）',
  `file_name`     VARCHAR(100) DEFAULT '' COMMENT '文件存储名（文件夹时为文件夹名）',
  `original_name` VARCHAR(255) DEFAULT '' COMMENT '原始文件名（文件夹时为空）',
  `file_path`     VARCHAR(500) DEFAULT '' COMMENT '文件存储路径（文件夹时为空）',
  `file_url`      VARCHAR(500) DEFAULT '' COMMENT '访问URL（文件夹时为空）',
  `file_size`     BIGINT       DEFAULT '0' COMMENT '文件大小（字节）',
  `file_type`     VARCHAR(50)  DEFAULT '' COMMENT '文件类型（image/document/video/audio/other，文件夹时为folder）',
  `storage_type`  VARCHAR(20)  DEFAULT 'local' COMMENT '存储类型（local/oss）',
  `is_folder`     TINYINT(1)   DEFAULT '0' COMMENT '是否文件夹（0=文件，1=文件夹）',
  `folder_id`     BIGINT       DEFAULT '0' COMMENT '父文件夹ID（0=根目录）',
  `scope`         VARCHAR(10)  DEFAULT 'admin' COMMENT '可见范围（admin=管理端可见，public=所有用户可见）',
  `sort_order`    INT          DEFAULT '0' COMMENT '排序号',
  `create_time`   DATETIME     DEFAULT NULL COMMENT '创建时间',
  `update_time`   DATETIME     DEFAULT NULL COMMENT '更新时间',
  `create_by`     BIGINT       DEFAULT NULL COMMENT '创建者',
  `update_by`     BIGINT       DEFAULT NULL COMMENT '更新者',
  `del_flag`      INT          DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  PRIMARY KEY (`id`),
  KEY `idx_folder_id` (`folder_id`),
  KEY `idx_scope` (`scope`),
  KEY `idx_is_folder` (`is_folder`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件管理记录（支持文件夹结构和可见范围）';

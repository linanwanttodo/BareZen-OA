-- ============================================
-- BareZen_Base V1: 核心表结构初始化
-- ============================================

-- 1. 用户表
CREATE TABLE IF NOT EXISTS `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `user_name` varchar(30) NOT NULL COMMENT '用户账号',
  `nick_name` varchar(30) NOT NULL COMMENT '用户昵称',
  `password` varchar(100) DEFAULT '' COMMENT '密码',
  `phone` varchar(11) DEFAULT '' COMMENT '手机号',
  `email` varchar(50) DEFAULT '' COMMENT '邮箱',
  `sex` char(1) DEFAULT '0' COMMENT '性别（0男 1女 2未知）',
  `avatar` varchar(500) DEFAULT '' COMMENT '头像路径',
  `status` char(1) DEFAULT '0' COMMENT '帐号状态（0正常 1停用）',
  `login_ip` varchar(64) DEFAULT '' COMMENT '最后登录IP',
  `login_date` datetime DEFAULT NULL COMMENT '最后登录时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建者ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新者ID',
  `del_flag` tinyint DEFAULT 0 COMMENT '逻辑删除（0=正常 1=已删除）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_name` (`user_name`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户信息表';

-- 2. 角色表
CREATE TABLE IF NOT EXISTS `sys_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_name` varchar(30) NOT NULL COMMENT '角色名称',
  `role_key` varchar(100) NOT NULL COMMENT '角色权限字符串',
  `role_sort` int NOT NULL COMMENT '显示顺序',
  `data_scope` tinyint DEFAULT 4 COMMENT '数据权限范围（1=仅本人 2=本部门 3=本部门及子部门 4=全部）',
  `status` char(1) NOT NULL COMMENT '角色状态（0正常 1停用）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建者ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新者ID',
  `del_flag` tinyint DEFAULT 0 COMMENT '逻辑删除',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_key` (`role_key`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色信息表';

-- 3. 菜单权限表
CREATE TABLE IF NOT EXISTS `sys_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
  `parent_id` bigint DEFAULT 0 COMMENT '父菜单ID（0=顶级菜单）',
  `menu_name` varchar(50) NOT NULL COMMENT '菜单名称',
  `menu_type` char(1) NOT NULL COMMENT '菜单类型（M目录 C菜单 F按钮）',
  `perms` varchar(100) DEFAULT NULL COMMENT '权限标识',
  `path` varchar(200) DEFAULT '' COMMENT '路由地址',
  `component` varchar(255) DEFAULT NULL COMMENT '组件路径',
  `icon` varchar(100) DEFAULT '#' COMMENT '菜单图标',
  `order_num` int DEFAULT 0 COMMENT '显示顺序',
  `visible` char(1) DEFAULT '1' COMMENT '是否可见（0=隐藏 1=显示）',
  `status` char(1) DEFAULT '1' COMMENT '菜单状态（0停用 1正常）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建者ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新者ID',
  `del_flag` tinyint DEFAULT 0 COMMENT '逻辑删除',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_perms` (`perms`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单权限表';

-- 4. 用户角色关联表
CREATE TABLE IF NOT EXISTS `sys_user_role` (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`user_id`, `role_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户和角色关联表';

-- 5. 角色菜单关联表
CREATE TABLE IF NOT EXISTS `sys_role_menu` (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `menu_id` bigint NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (`role_id`, `menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色和菜单关联表';

-- 6. 用户-部门关联表
CREATE TABLE IF NOT EXISTS `sys_user_dept` (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `dept_id` bigint NOT NULL COMMENT '部门ID',
  `role_type` char(1) DEFAULT '0' COMMENT '部门内角色（0=普通用户 1=管理员）',
  PRIMARY KEY (`user_id`, `dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户和部门关联表';

-- 7. 部门表
CREATE TABLE IF NOT EXISTS `sys_dept` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '部门ID',
  `parent_id` bigint DEFAULT 0 COMMENT '父部门ID（0=顶级部门）',
  `dept_name` varchar(50) NOT NULL COMMENT '部门名称',
  `sort` int DEFAULT 0 COMMENT '显示顺序',
  `leader` varchar(50) DEFAULT NULL COMMENT '负责人',
  `phone` varchar(11) DEFAULT NULL COMMENT '联系电话',
  `email` varchar(50) DEFAULT NULL COMMENT '邮箱',
  `status` char(1) DEFAULT '0' COMMENT '部门状态（0=正常 1=停用）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `del_flag` tinyint DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- ============================================
-- BareZen_Base RBAC 核心权限体系初始化脚本
-- ============================================

-- 1. 用户表
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `user_name` varchar(30) NOT NULL COMMENT '用户账号',
  `nick_name` varchar(30) NOT NULL COMMENT '用户昵称',
  `password` varchar(100) DEFAULT '' COMMENT '密码',
  `phone` varchar(11) DEFAULT '' COMMENT '手机号',
  `email` varchar(50) DEFAULT '' COMMENT '邮箱',
  `sex` char(1) DEFAULT '0' COMMENT '性别（0男 1女 2未知）',
  `status` char(1) DEFAULT '1' COMMENT '帐号状态（0正常 1停用）',
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
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_name` varchar(30) NOT NULL COMMENT '角色名称',
  `role_key` varchar(100) NOT NULL COMMENT '角色权限字符串',
  `role_sort` int NOT NULL COMMENT '显示顺序',
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
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
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
  `status` char(1) DEFAULT '1' COMMENT '状态（0停用 1正常）',
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
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`user_id`, `role_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户和角色关联表';

-- 5. 角色菜单关联表
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu` (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `menu_id` bigint NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (`role_id`, `menu_id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_menu_id` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色和菜单关联表';

-- ============================================
-- 初始化数据
-- ============================================

-- 插入角色
INSERT INTO `sys_role` VALUES (1, '超级管理员', 'admin', 1, '0', NOW(), NOW(), NULL, NULL, 0, '超级管理员拥有所有权限');
INSERT INTO `sys_role` VALUES (2, '普通用户', 'common', 2, '0', NOW(), NOW(), NULL, NULL, 0, '普通用户');

-- 插入管理员用户 (密码: admin123, BCrypt加密后)
-- 状态: '0'=正常, '1'=停用
INSERT INTO `sys_user` VALUES (1, 'admin', '超级管理员', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '13888888888', 'admin@barezen.com', '0', '0', '', NULL, NOW(), NOW(), NULL, NULL, 0, '超级管理员');

-- 插入菜单
-- ID=1: 系统管理目录 (M)
INSERT INTO `sys_menu` VALUES (1, 0, '系统管理', 'M', NULL, 'system', NULL, 'system', 1, '0', '1', NOW(), NOW(), NULL, NULL, 0, '系统管理目录');
-- ID=2: 用户管理菜单 (C)
INSERT INTO `sys_menu` VALUES (2, 1, '用户管理', 'C', 'system:user:list', 'user', 'system/user/index', 'user', 1, '0', '1', NOW(), NOW(), NULL, NULL, 0, '用户管理菜单');
-- ID=3: 角色管理菜单 (C)
INSERT INTO `sys_menu` VALUES (3, 1, '角色管理', 'C', 'system:role:list', 'role', 'system/role/index', 'peoples', 2, '0', '1', NOW(), NOW(), NULL, NULL, 0, '角色管理菜单');
-- ID=4: 菜单管理菜单 (C)
INSERT INTO `sys_menu` VALUES (4, 1, '菜单管理', 'C', 'system:menu:list', 'menu', 'system/menu/index', 'tree-table', 3, '0', '1', NOW(), NOW(), NULL, NULL, 0, '菜单管理菜单');
-- ID=5: 内容管理目录 (M)
INSERT INTO `sys_menu` VALUES (5, 0, '内容管理', 'M', NULL, 'content', NULL, 'catalog', 2, '0', '1', NOW(), NOW(), NULL, NULL, 0, '内容管理目录');
-- ID=6: 文章管理菜单 (C)
INSERT INTO `sys_menu` VALUES (6, 5, '文章管理', 'C', 'content:article:list', 'article', 'content/article/index', 'catalog', 1, '0', '1', NOW(), NOW(), NULL, NULL, 0, '文章管理菜单');

-- 关联：admin 用户拥有 admin 角色
INSERT INTO `sys_user_role` VALUES (1, 1);

-- 关联：admin 角色拥有所有菜单权限 (admin 在代码中硬编码放行，但这里也配置以备扩展)
INSERT INTO `sys_role_menu` VALUES (1, 1);
INSERT INTO `sys_role_menu` VALUES (1, 2);
INSERT INTO `sys_role_menu` VALUES (1, 3);
INSERT INTO `sys_role_menu` VALUES (1, 4);
INSERT INTO `sys_role_menu` VALUES (1, 5);
INSERT INTO `sys_role_menu` VALUES (1, 6);

-- ============================================
-- 修正自增起始值
-- 避免手动插入的 ID(1,2,3,4) 与雪花算法生成的 ID 冲突
-- 雪花算法生成的 ID 约为 1.3e15 起，所以设置为 10000 足够
-- ============================================
ALTER TABLE `sys_user` AUTO_INCREMENT = 10000;
ALTER TABLE `sys_role` AUTO_INCREMENT = 10000;
ALTER TABLE `sys_menu` AUTO_INCREMENT = 10000;

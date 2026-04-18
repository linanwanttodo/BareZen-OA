-- ============================================
-- BareZen_Base 数据字典表
-- ============================================

-- 字典类型表
DROP TABLE IF EXISTS `sys_dict_type`;
CREATE TABLE `sys_dict_type` (
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

-- 字典数据表
DROP TABLE IF EXISTS `sys_dict_data`;
CREATE TABLE `sys_dict_data` (
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

-- ============================================
-- 初始化字典数据
-- ============================================

-- 字典类型
INSERT INTO `sys_dict_type` VALUES (1, '用户性别', 'sys_user_sex', '1', NOW(), NOW(), NULL, NULL, 0, '用户性别列表');
INSERT INTO `sys_dict_type` VALUES (2, '菜单状态', 'sys_show_hide', '1', NOW(), NOW(), NULL, NULL, 0, '菜单状态列表');
INSERT INTO `sys_dict_type` VALUES (3, '系统开关', 'sys_normal_disable', '1', NOW(), NOW(), NULL, NULL, 0, '系统开关列表');
INSERT INTO `sys_dict_type` VALUES (4, '通知类型', 'sys_notice_type', '1', NOW(), NOW(), NULL, NULL, 0, '通知公告类型');
INSERT INTO `sys_dict_type` VALUES (5, '操作类型', 'sys_oper_type', '1', NOW(), NOW(), NULL, NULL, 0, '操作日志操作类型');

-- 字典数据：用户性别
INSERT INTO `sys_dict_data` VALUES (1, 1, '男', '0', 'sys_user_sex', '', 'primary', 'Y', '1', NOW(), NOW(), NULL, NULL, 0, '男性');
INSERT INTO `sys_dict_data` VALUES (2, 2, '女', '1', 'sys_user_sex', '', 'danger', 'N', '1', NOW(), NOW(), NULL, NULL, 0, '女性');
INSERT INTO `sys_dict_data` VALUES (3, 3, '未知', '2', 'sys_user_sex', '', 'info', 'N', '1', NOW(), NOW(), NULL, NULL, 0, '未知性别');

-- 字典数据：菜单状态
INSERT INTO `sys_dict_data` VALUES (4, 1, '显示', '0', 'sys_show_hide', '', 'danger', 'Y', '1', NOW(), NOW(), NULL, NULL, 0, '菜单显示');
INSERT INTO `sys_dict_data` VALUES (5, 2, '隐藏', '1', 'sys_show_hide', '', 'warning', 'N', '1', NOW(), NOW(), NULL, NULL, 0, '菜单隐藏');

-- 字典数据：系统开关
INSERT INTO `sys_dict_data` VALUES (6, 1, '正常', '1', 'sys_normal_disable', '', 'primary', 'Y', '1', NOW(), NOW(), NULL, NULL, 0, '正常状态');
INSERT INTO `sys_dict_data` VALUES (7, 2, '停用', '0', 'sys_normal_disable', '', 'danger', 'N', '1', NOW(), NOW(), NULL, NULL, 0, '停用状态');

-- 字典数据：通知类型
INSERT INTO `sys_dict_data` VALUES (8, 1, '通知', '1', 'sys_notice_type', '', 'warning', 'Y', '1', NOW(), NOW(), NULL, NULL, 0, '弹窗通知');
INSERT INTO `sys_dict_data` VALUES (9, 2, '公告', '2', 'sys_notice_type', '', 'info', 'Y', '1', NOW(), NOW(), NULL, NULL, 0, '首页公告');

-- 字典数据：操作类型
INSERT INTO `sys_dict_data` VALUES (10, 1, '新增', '1', 'sys_oper_type', '', 'success', 'Y', '1', NOW(), NOW(), NULL, NULL, 0, '新增操作');
INSERT INTO `sys_dict_data` VALUES (11, 2, '修改', '2', 'sys_oper_type', '', 'warning', 'Y', '1', NOW(), NOW(), NULL, NULL, 0, '修改操作');
INSERT INTO `sys_dict_data` VALUES (12, 3, '删除', '3', 'sys_oper_type', '', 'danger', 'Y', '1', NOW(), NOW(), NULL, NULL, 0, '删除操作');
INSERT INTO `sys_dict_data` VALUES (13, 4, '授权', '4', 'sys_oper_type', '', 'primary', 'Y', '1', NOW(), NOW(), NULL, NULL, 0, '授权操作');
INSERT INTO `sys_dict_data` VALUES (14, 5, '导出', '5', 'sys_oper_type', '', 'success', 'Y', '1', NOW(), NOW(), NULL, NULL, 0, '导出操作');
INSERT INTO `sys_dict_data` VALUES (15, 6, '导入', '6', 'sys_oper_type', '', 'success', 'Y', '1', NOW(), NOW(), NULL, NULL, 0, '导入操作');
INSERT INTO `sys_dict_data` VALUES (16, 7, '强退', '7', 'sys_oper_type', '', 'danger', 'Y', '1', NOW(), NOW(), NULL, NULL, 0, '强退操作');
INSERT INTO `sys_dict_data` VALUES (17, 8, '清空数据', '8', 'sys_oper_type', '', 'danger', 'Y', '1', NOW(), NOW(), NULL, NULL, 0, '清空数据操作');
INSERT INTO `sys_dict_data` VALUES (18, 9, '其他', '0', 'sys_oper_type', '', 'info', 'Y', '1', NOW(), NOW(), NULL, NULL, 0, '其他操作');

-- 自增起始值
ALTER TABLE `sys_dict_type` AUTO_INCREMENT = 10000;
ALTER TABLE `sys_dict_data` AUTO_INCREMENT = 10000;

-- ============================================
-- BareZen_Base V16: 员工自助申请（加班/出差）
-- ============================================
-- 公司内部管理系统：员工前台自助提交加班/出差申请，后台审批流。

-- 1. 加班申请表
CREATE TABLE IF NOT EXISTS `att_overtime_request` (
  `id` BIGINT NOT NULL COMMENT '主键ID（雪花ID）',
  `user_id` BIGINT NOT NULL COMMENT '申请人用户ID',
  `user_name` VARCHAR(50) DEFAULT NULL COMMENT '申请人姓名（冗余）',
  `dept_name` VARCHAR(100) DEFAULT NULL COMMENT '申请人部门（冗余）',
  `overtime_date` VARCHAR(20) DEFAULT NULL COMMENT '加班日期（yyyy-MM-dd）',
  `start_time` VARCHAR(20) NOT NULL COMMENT '开始时间（yyyy-MM-dd HH:mm）',
  `end_time` VARCHAR(20) NOT NULL COMMENT '结束时间（yyyy-MM-dd HH:mm）',
  `duration_hours` DECIMAL(5,1) DEFAULT 0 COMMENT '加班时长（小时）',
  `reason` VARCHAR(500) DEFAULT NULL COMMENT '加班事由',
  `status` VARCHAR(4) NOT NULL DEFAULT '0' COMMENT '状态：0=待审批 1=已通过 2=已驳回 3=已取消',
  `approver_id` BIGINT DEFAULT NULL COMMENT '审批人用户ID',
  `approver_name` VARCHAR(50) DEFAULT NULL COMMENT '审批人姓名',
  `approve_time` VARCHAR(20) DEFAULT NULL COMMENT '审批时间',
  `approve_remark` VARCHAR(500) DEFAULT NULL COMMENT '审批意见',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建者ID',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新者ID',
  `del_flag` TINYINT DEFAULT 0 COMMENT '逻辑删除：0=未删 1=已删',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='加班申请表';

-- 2. 出差申请表
CREATE TABLE IF NOT EXISTS `att_travel_request` (
  `id` BIGINT NOT NULL COMMENT '主键ID（雪花ID）',
  `user_id` BIGINT NOT NULL COMMENT '申请人用户ID',
  `user_name` VARCHAR(50) DEFAULT NULL COMMENT '申请人姓名（冗余）',
  `dept_name` VARCHAR(100) DEFAULT NULL COMMENT '申请人部门（冗余）',
  `destination` VARCHAR(200) DEFAULT NULL COMMENT '目的地',
  `purpose` VARCHAR(500) DEFAULT NULL COMMENT '出差事由',
  `start_time` VARCHAR(20) NOT NULL COMMENT '开始时间（yyyy-MM-dd HH:mm）',
  `end_time` VARCHAR(20) NOT NULL COMMENT '结束时间（yyyy-MM-dd HH:mm）',
  `days` DECIMAL(5,1) DEFAULT 0 COMMENT '出差天数',
  `status` VARCHAR(4) NOT NULL DEFAULT '0' COMMENT '状态：0=待审批 1=已通过 2=已驳回 3=已取消',
  `approver_id` BIGINT DEFAULT NULL COMMENT '审批人用户ID',
  `approver_name` VARCHAR(50) DEFAULT NULL COMMENT '审批人姓名',
  `approve_time` VARCHAR(20) DEFAULT NULL COMMENT '审批时间',
  `approve_remark` VARCHAR(500) DEFAULT NULL COMMENT '审批意见',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建者ID',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新者ID',
  `del_flag` TINYINT DEFAULT 0 COMMENT '逻辑删除：0=未删 1=已删',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='出差申请表';

-- 3. 菜单：加班申请（id=10010）、出差申请（id=10011），挂载在人事管理目录（id=10001）下
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `perms`, `path`, `component`, `icon`, `order_num`, `visible`, `status`, `remark`) VALUES
(10010, 10001, '加班申请', 'C', 'hr:overtime:list', 'hr/overtime', 'hr/overtime/index', 'clock', 3, '0', '1', '加班申请菜单'),
(10011, 10001, '出差申请', 'C', 'hr:travel:list', 'hr/travel', 'hr/travel/index', 'flight', 4, '0', '1', '出差申请菜单');

-- 4. F 类型按钮权限
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `perms`, `path`, `component`, `icon`, `order_num`, `visible`, `status`, `remark`) VALUES
(10229, 10010, '加班申请提交', 'F', 'hr:overtime:add', NULL, NULL, '#', 1, '0', '1', '加班申请提交按钮'),
(10230, 10010, '加班申请审批', 'F', 'hr:overtime:approve', NULL, NULL, '#', 2, '0', '1', '加班申请审批按钮'),
(10231, 10010, '加班申请撤销', 'F', 'hr:overtime:edit', NULL, NULL, '#', 3, '0', '1', '加班申请撤销按钮'),
(10232, 10010, '加班申请删除', 'F', 'hr:overtime:delete', NULL, NULL, '#', 4, '0', '1', '加班申请删除按钮'),
(10233, 10011, '出差申请提交', 'F', 'hr:travel:add', NULL, NULL, '#', 1, '0', '1', '出差申请提交按钮'),
(10234, 10011, '出差申请审批', 'F', 'hr:travel:approve', NULL, NULL, '#', 2, '0', '1', '出差申请审批按钮'),
(10235, 10011, '出差申请撤销', 'F', 'hr:travel:edit', NULL, NULL, '#', 3, '0', '1', '出差申请撤销按钮'),
(10236, 10011, '出差申请删除', 'F', 'hr:travel:delete', NULL, NULL, '#', 4, '0', '1', '出差申请删除按钮');

-- 5. admin 角色授权
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(1, 10010), (1, 10011),
(1, 10229), (1, 10230), (1, 10231), (1, 10232),
(1, 10233), (1, 10234), (1, 10235), (1, 10236);

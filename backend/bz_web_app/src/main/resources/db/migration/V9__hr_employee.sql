-- ============================================
-- BareZen_Base V9: 人事管理模块（员工档案）
-- ============================================
-- 公司内部管理系统：员工档案管理，覆盖入职/在职/离职全生命周期。

-- 1. 员工档案表
CREATE TABLE IF NOT EXISTS `hr_employee` (
  `id` BIGINT NOT NULL COMMENT '主键ID（雪花ID）',
  `emp_no` VARCHAR(32) NOT NULL COMMENT '员工工号',
  `name` VARCHAR(50) NOT NULL COMMENT '姓名',
  `gender` VARCHAR(4) DEFAULT '2' COMMENT '性别：0=男 1=女 2=未知',
  `dept_id` BIGINT DEFAULT NULL COMMENT '所属部门ID',
  `dept_name` VARCHAR(100) DEFAULT NULL COMMENT '所属部门名称（冗余，便于列表展示）',
  `position` VARCHAR(50) DEFAULT NULL COMMENT '职位',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `id_card` VARCHAR(18) DEFAULT NULL COMMENT '身份证号（脱敏存储）',
  `hire_date` VARCHAR(20) DEFAULT NULL COMMENT '入职日期',
  `leave_date` VARCHAR(20) DEFAULT NULL COMMENT '离职日期',
  `employment_type` VARCHAR(10) DEFAULT '0' COMMENT '用工类型：0=全职 1=实习 2=兼职 3=劳务',
  `education` VARCHAR(20) DEFAULT NULL COMMENT '学历：高中/大专/本科/硕士/博士',
  `status` VARCHAR(4) DEFAULT '0' COMMENT '状态：0=试用 1=在职 2=离职',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建者ID',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新者ID',
  `del_flag` TINYINT DEFAULT 0 COMMENT '逻辑删除：0=未删 1=已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_emp_no` (`emp_no`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_status` (`status`),
  KEY `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='员工档案表';

-- 2. 人事管理菜单（id=14 目录, id=15 员工档案菜单）
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `perms`, `path`, `component`, `icon`, `order_num`, `visible`, `status`, `remark`) VALUES
(14, 0, '人事管理', 'M', NULL, 'hr', NULL, 'user', 5, '0', '1', '人事管理目录'),
(15, 14, '员工档案', 'C', 'hr:employee:list', 'employee', 'hr/employee/index', 'peoples', 1, '0', '1', '员工档案菜单');

-- 3. admin 角色授予人事管理权限
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(1, 14), (1, 15);

-- 4. 初始员工数据（与现有 admin 用户对应）
INSERT IGNORE INTO `hr_employee` (`id`, `emp_no`, `name`, `gender`, `dept_id`, `dept_name`, `position`, `phone`, `email`, `hire_date`, `employment_type`, `education`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `del_flag`)
VALUES (1, 'EMP0001', '超级管理员', '2', 1, '总公司', '系统管理员', '13888888888', 'admin@barezen.com', '2025-01-01', '0', '本科', '1', NOW(), NOW(), 1, 1, 0);

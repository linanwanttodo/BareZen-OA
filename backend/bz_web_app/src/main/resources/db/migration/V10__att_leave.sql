-- ============================================
-- BareZen_Base V10: 考勤与请假审批模块
-- ============================================
-- 公司内部管理系统：请假申请与审批流程。

-- 1. 请假申请表
CREATE TABLE IF NOT EXISTS `att_leave_request` (
  `id` BIGINT NOT NULL COMMENT '主键ID（雪花ID）',
  `user_id` BIGINT NOT NULL COMMENT '申请人用户ID',
  `user_name` VARCHAR(50) DEFAULT NULL COMMENT '申请人姓名（冗余）',
  `dept_name` VARCHAR(100) DEFAULT NULL COMMENT '申请人部门（冗余）',
  `leave_type` VARCHAR(10) NOT NULL DEFAULT 'other' COMMENT '请假类型：annual=年假 sick=病假 personal=事假 overtime=调休 marriage=婚假 maternity=产假 other=其他',
  `start_time` VARCHAR(20) NOT NULL COMMENT '开始时间（yyyy-MM-dd HH:mm）',
  `end_time` VARCHAR(20) NOT NULL COMMENT '结束时间（yyyy-MM-dd HH:mm）',
  `leave_days` DECIMAL(5,1) DEFAULT 0 COMMENT '请假天数',
  `reason` VARCHAR(500) DEFAULT NULL COMMENT '请假事由',
  `status` VARCHAR(4) NOT NULL DEFAULT '0' COMMENT '状态：0=待审批 1=已通过 2=已驳回 3=已撤销',
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
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='请假申请表';

-- 2. 请假管理菜单（id=16，挂载在人事管理目录 id=14 下）
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `perms`, `path`, `component`, `icon`, `order_num`, `visible`, `status`, `remark`) VALUES
(16, 14, '请假审批', 'C', 'hr:leave:list', 'leave', 'hr/leave/index', 'calendar', 2, '0', '1', '请假审批菜单');

-- 3. admin 角色授予请假管理权限
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (1, 16);

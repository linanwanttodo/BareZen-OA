-- ============================================
-- BareZen_Base V12: 会议与行政模块
-- ============================================
-- 公司内部管理系统：会议室预订、用车申请。

-- 1. 会议室表
CREATE TABLE IF NOT EXISTS `office_meeting_room` (
  `id` BIGINT NOT NULL COMMENT '主键ID（雪花ID）',
  `room_name` VARCHAR(100) NOT NULL COMMENT '会议室名称',
  `capacity` INT DEFAULT 0 COMMENT '容纳人数',
  `location` VARCHAR(100) DEFAULT NULL COMMENT '位置',
  `equipment` VARCHAR(255) DEFAULT NULL COMMENT '设备（投影/白板/视频会议，逗号分隔）',
  `status` VARCHAR(4) NOT NULL DEFAULT '0' COMMENT '状态：0=可用 1=停用',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建者ID',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新者ID',
  `del_flag` TINYINT DEFAULT 0 COMMENT '逻辑删除：0=未删 1=已删',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会议室表';

-- 2. 会议预订表
CREATE TABLE IF NOT EXISTS `office_meeting_booking` (
  `id` BIGINT NOT NULL COMMENT '主键ID（雪花ID）',
  `room_id` BIGINT NOT NULL COMMENT '会议室ID',
  `room_name` VARCHAR(100) DEFAULT NULL COMMENT '会议室名称（冗余）',
  `title` VARCHAR(100) NOT NULL COMMENT '会议主题',
  `booker_id` BIGINT DEFAULT NULL COMMENT '预订人用户ID',
  `booker_name` VARCHAR(50) DEFAULT NULL COMMENT '预订人姓名',
  `start_time` VARCHAR(20) NOT NULL COMMENT '开始时间（yyyy-MM-dd HH:mm）',
  `end_time` VARCHAR(20) NOT NULL COMMENT '结束时间（yyyy-MM-dd HH:mm）',
  `attendees` VARCHAR(255) DEFAULT NULL COMMENT '参会人员（逗号分隔）',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建者ID',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新者ID',
  `del_flag` TINYINT DEFAULT 0 COMMENT '逻辑删除：0=未删 1=已删',
  PRIMARY KEY (`id`),
  KEY `idx_room_id` (`room_id`),
  KEY `idx_start_time` (`start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会议预订表';

-- 3. 用车申请表
CREATE TABLE IF NOT EXISTS `office_vehicle_request` (
  `id` BIGINT NOT NULL COMMENT '主键ID（雪花ID）',
  `applicant_id` BIGINT DEFAULT NULL COMMENT '申请人用户ID',
  `applicant_name` VARCHAR(50) DEFAULT NULL COMMENT '申请人姓名',
  `dept_name` VARCHAR(100) DEFAULT NULL COMMENT '申请部门',
  `vehicle_type` VARCHAR(20) DEFAULT 'car' COMMENT '车型：car=轿车 van=商务车 bus=客车 other=其他',
  `purpose` VARCHAR(200) NOT NULL COMMENT '用车事由',
  `start_time` VARCHAR(20) NOT NULL COMMENT '用车开始时间',
  `end_time` VARCHAR(20) NOT NULL COMMENT '用车结束时间',
  `destination` VARCHAR(200) DEFAULT NULL COMMENT '目的地',
  `passenger_count` INT DEFAULT 1 COMMENT '乘车人数',
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
  KEY `idx_applicant_id` (`applicant_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用车申请表';

-- 4. 行政办公菜单（id=19 目录, 20=会议室, 21=用车申请）
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `perms`, `path`, `component`, `icon`, `order_num`, `visible`, `status`, `remark`) VALUES
(19, 0, '行政办公', 'M', NULL, 'office', NULL, 'calendar', 7, '0', '1', '行政办公目录'),
(20, 19, '会议室预订', 'C', 'office:meeting:list', 'meeting', 'office/meeting/index', 'apps', 1, '0', '1', '会议室预订菜单'),
(21, 19, '用车申请', 'C', 'office:vehicle:list', 'vehicle', 'office/vehicle/index', 'drive', 2, '0', '1', '用车申请菜单');

-- 5. admin 角色授予行政办公权限
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (1, 19), (1, 20), (1, 21);

-- 6. 示例会议室
INSERT IGNORE INTO `office_meeting_room` (`id`, `room_name`, `capacity`, `location`, `equipment`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `del_flag`) VALUES
(1, '一号会议室', 10, '3F-301', '投影,白板', '0', NOW(), NOW(), 1, 1, 0),
(2, '二号会议室', 20, '3F-302', '投影,视频会议', '0', NOW(), NOW(), 1, 1, 0);

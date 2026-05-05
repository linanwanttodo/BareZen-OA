-- ============================================
-- BareZen_Base V34: 考勤打卡模块
-- ============================================
-- 公司内部管理系统：上下班打卡记录与考勤规则配置。
-- 打卡记录与同域 att_leave/att_overtime/att_travel 保持一致的
-- user_id + user_name + dept_name 冗余模式；同一用户同日仅一行，
-- 上班/下班打卡通过更新该行完成，重复打卡由服务端拒绝。

-- 1. 打卡记录表
CREATE TABLE IF NOT EXISTS `att_clock_record` (
  `id` BIGINT NOT NULL COMMENT '主键ID（雪花ID）',
  `user_id` BIGINT NOT NULL COMMENT '打卡人用户ID',
  `user_name` VARCHAR(50) DEFAULT NULL COMMENT '打卡人姓名（冗余）',
  `dept_name` VARCHAR(100) DEFAULT NULL COMMENT '打卡人部门（冗余）',
  `clock_date` DATE NOT NULL COMMENT '打卡日期',
  `clock_in_time` DATETIME DEFAULT NULL COMMENT '上班打卡时间',
  `clock_out_time` DATETIME DEFAULT NULL COMMENT '下班打卡时间',
  `late_minutes` INT NOT NULL DEFAULT 0 COMMENT '迟到分钟数（服务端计算）',
  `early_minutes` INT NOT NULL DEFAULT 0 COMMENT '早退分钟数（服务端计算）',
  `status` VARCHAR(10) NOT NULL DEFAULT 'normal' COMMENT '状态：normal=正常 late=迟到 early=早退 absent=缺卡 abnormal=异常',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建者ID',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新者ID',
  `del_flag` TINYINT DEFAULT 0 COMMENT '逻辑删除：0=未删 1=已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_clock_date` (`user_id`, `clock_date`, `del_flag`),
  KEY `idx_clock_date` (`clock_date`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='考勤打卡记录表';

-- 2. 考勤规则配置（走 sys_config，改配置不改代码）
-- sys_config id 1-5 已被 V3 占用，此处从 6 起。
INSERT IGNORE INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `status`, `remark`) VALUES
(6, '考勤上班时间', 'hr.attendance.work_start_time', '09:00', 'Y', '1', '考勤上班时间（HH:mm）'),
(7, '考勤下班时间', 'hr.attendance.work_end_time', '18:00', 'Y', '1', '考勤下班时间（HH:mm）'),
(8, '迟到宽容分钟数', 'hr.attendance.late_grace_minutes', '0', 'Y', '1', '迟到宽容分钟数（0=不宽容，晚于上班时间+宽容分钟即记迟到）');
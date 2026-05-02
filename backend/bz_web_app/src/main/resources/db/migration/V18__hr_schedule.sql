-- ============================================
-- BareZen_Base V18: 日程管理
-- ============================================
-- 公司内部管理系统：员工个人日程安排，支持提醒时间。

-- 1. 个人日程表
CREATE TABLE IF NOT EXISTS `hr_schedule` (
  `id` BIGINT NOT NULL COMMENT '主键ID（雪花ID）',
  `user_id` BIGINT NOT NULL COMMENT '所属用户ID',
  `user_name` VARCHAR(50) DEFAULT NULL COMMENT '所属用户姓名（冗余）',
  `title` VARCHAR(200) NOT NULL COMMENT '日程标题',
  `description` VARCHAR(1000) DEFAULT NULL COMMENT '日程描述',
  `start_time` VARCHAR(20) NOT NULL COMMENT '开始时间（yyyy-MM-dd HH:mm）',
  `end_time` VARCHAR(20) NOT NULL COMMENT '结束时间（yyyy-MM-dd HH:mm）',
  `remind_time` VARCHAR(20) DEFAULT NULL COMMENT '提醒时间（yyyy-MM-dd HH:mm，null=不提醒）',
  `status` VARCHAR(4) NOT NULL DEFAULT '0' COMMENT '状态：0=未开始 1=进行中 2=已完成 3=已取消',
  `location` VARCHAR(100) DEFAULT NULL COMMENT '地点',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建者ID',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新者ID',
  `del_flag` TINYINT DEFAULT 0 COMMENT '逻辑删除：0=未删 1=已删',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_start_time` (`start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='个人日程表';

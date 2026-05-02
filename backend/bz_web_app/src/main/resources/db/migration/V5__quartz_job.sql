-- ============================================
-- BareZen_Base V5: Quartz 定时任务 + 任务日志
-- ============================================

-- 1. 定时任务表（业务管理用）
CREATE TABLE IF NOT EXISTS `sys_job` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `job_name` varchar(64) NOT NULL COMMENT '任务名称',
  `job_group` varchar(64) DEFAULT 'DEFAULT' COMMENT '任务组名',
  `invoke_target` varchar(500) NOT NULL COMMENT '调用目标（Spring Bean 方法名）',
  `cron_expression` varchar(255) DEFAULT '' COMMENT 'Cron 执行表达式',
  `misfire_policy` varchar(20) DEFAULT '3' COMMENT '计划执行错误策略（1=立即执行 2=过期执行一次 3=放弃）',
  `concurrent` char(1) DEFAULT '1' COMMENT '是否并发执行（0=禁止 1=允许）',
  `status` char(1) DEFAULT '0' COMMENT '状态（0=暂停 1=正常）',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `del_flag` tinyint DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定时任务调度表';

-- 2. 任务执行日志表
CREATE TABLE IF NOT EXISTS `sys_job_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `job_id` bigint NOT NULL COMMENT '任务ID',
  `job_name` varchar(64) NOT NULL COMMENT '任务名称',
  `job_group` varchar(64) DEFAULT 'DEFAULT' COMMENT '任务组名',
  `invoke_target` varchar(500) NOT NULL COMMENT '调用目标',
  `cron_expression` varchar(255) DEFAULT '' COMMENT 'Cron表达式',
  `status` char(1) DEFAULT '0' COMMENT '状态（0=成功 1=失败）',
  `error_msg` varchar(2000) DEFAULT '' COMMENT '错误消息',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `stop_time` datetime DEFAULT NULL COMMENT '结束时间',
  `duration` bigint DEFAULT 0 COMMENT '耗时（毫秒）',
  PRIMARY KEY (`id`),
  KEY `idx_job_id` (`job_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定时任务执行日志表';

-- 插入默认任务（清理操作日志，每天凌晨2点执行）
INSERT IGNORE INTO `sys_job` (`id`, `job_name`, `job_group`, `invoke_target`, `cron_expression`, `status`, `remark`) VALUES
(1, '清理过期操作日志', 'DEFAULT', 'cleanTask.cleanOperLog()', '0 0 2 * * ?', '1', '每天凌晨2点清理30天前的操作日志');

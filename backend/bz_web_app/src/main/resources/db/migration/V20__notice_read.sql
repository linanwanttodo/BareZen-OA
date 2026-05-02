-- ============================================
-- BareZen_Base V20: 公告已读回执
-- ============================================
-- 公司内部管理系统：公告支持强制阅读，并追踪每个用户的已读状态。

-- 1. sys_notice 增加强制阅读标记
ALTER TABLE `sys_notice`
  ADD COLUMN `require_read` char(1) DEFAULT 'N' COMMENT '是否强制阅读（Y=是 N=否）' AFTER `scope`;

-- 2. 公告已读记录表
CREATE TABLE IF NOT EXISTS `sys_notice_read` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `notice_id` BIGINT NOT NULL COMMENT '公告ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `read_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '阅读时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notice_user` (`notice_id`, `user_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公告已读记录表';

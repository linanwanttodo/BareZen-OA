-- ============================================
-- BareZen_Base V4: 系统增强
-- ============================================

-- 1. 登录日志表（记录所有登录尝试）
CREATE TABLE IF NOT EXISTS `sys_logininfor` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '访问ID',
  `user_name` varchar(50) DEFAULT '' COMMENT '用户账号',
  `ip_addr` varchar(128) DEFAULT '' COMMENT '登录IP',
  `status` char(1) DEFAULT '0' COMMENT '状态（0=成功 1=失败）',
  `msg` varchar(255) DEFAULT '' COMMENT '提示消息',
  `browser` varchar(50) DEFAULT '' COMMENT '浏览器',
  `os` varchar(50) DEFAULT '' COMMENT '操作系统',
  `login_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_name` (`user_name`),
  KEY `idx_login_time` (`login_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表';

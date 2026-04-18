-- ============================================
-- BareZen_Base 通知公告表
-- ============================================

DROP TABLE IF EXISTS `sys_notice`;
CREATE TABLE `sys_notice` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `notice_title` varchar(128) NOT NULL COMMENT '通知标题',
  `notice_content` text COMMENT '通知内容（HTML富文本）',
  `notice_type` char(1) DEFAULT '2' COMMENT '类型：1=通知（弹窗） 2=公告（首页展示）',
  `status` char(1) DEFAULT '0' COMMENT '状态：0=草稿 1=已发布 2=已下架',
  `scope` varchar(10) DEFAULT 'public' COMMENT '可见范围：public=全部用户 internal=仅站内用户',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建者ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新者ID',
  `del_flag` tinyint DEFAULT 0 COMMENT '逻辑删除（0=正常 1=已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_type_status` (`notice_type`, `status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知公告表';

-- 初始化一条公告数据
INSERT INTO `sys_notice` (`id`, `notice_title`, `notice_content`, `notice_type`, `status`, `scope`, `create_time`, `update_time`, `create_by`, `update_by`, `del_flag`)
VALUES (1, '欢迎使用 BareZen_Base', '<p>BareZen_Base 是一个现代化的后台管理系统，采用 Spring Boot + Vue 3 技术栈构建。</p><p>系统默认管理员账号：admin / admin123</p>', '2', '1', 'public', NOW(), NOW(), NULL, NULL, 0);

-- ============================================
-- BareZen_Base V44: 站内消息表（3.1 邮件+站内信）
-- ============================================
-- 背景：WebSocket 在线推送只能触达在线用户，离线时通知丢失。
--       本迁移新建 sys_message 站内信表作为离线兜底：
--         审批完成 / 新的待办等事件，WS 推送失败（用户离线）时落库，
--         用户登录后通过消息中心（/api/app/v1/messages）查看与标记已读。
-- 说明：与 sys_notice（公告，全员广播）不同，sys_message 为按用户定向消息。

CREATE TABLE IF NOT EXISTS `sys_message` (
  `id` bigint NOT NULL COMMENT '主键ID（雪花算法）',
  `user_id` bigint NOT NULL COMMENT '收件人用户ID',
  `title` varchar(200) NOT NULL COMMENT '消息标题',
  `content` varchar(1000) DEFAULT '' COMMENT '消息内容',
  `msg_type` varchar(30) NOT NULL DEFAULT 'system' COMMENT '消息类型（approval=审批通知/system=系统消息/notice=公告）',
  `biz_type` varchar(30) DEFAULT NULL COMMENT '关联业务类型（leave/overtime/travel/vehicle/supply）',
  `biz_id` bigint DEFAULT NULL COMMENT '关联业务单ID',
  `is_read` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已读（0=未读 1=已读）',
  `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `del_flag` tinyint(1) DEFAULT '0' COMMENT '逻辑删除（0=正常 1=已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`, `create_time`),
  KEY `idx_user_read` (`user_id`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='站内消息表（WebSocket 离线兜底）';
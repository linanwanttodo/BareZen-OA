-- ============================================
-- BareZen_Base V22: 审批流程历史（进度追踪）
-- ============================================
-- 记录各业务申请单的流程节点（提交/审批/撤销），供前端展示办事进程图。

CREATE TABLE IF NOT EXISTS `approval_flow_log` (
  `id` BIGINT NOT NULL COMMENT '主键ID（雪花ID）',
  `biz_type` VARCHAR(30) NOT NULL COMMENT '业务类型：leave=请假 overtime=加班 travel=出差 vehicle=用车 supply=用品申领',
  `biz_id` BIGINT NOT NULL COMMENT '业务记录ID',
  `node_name` VARCHAR(50) NOT NULL COMMENT '节点名称：提交申请/审批通过/审批驳回/撤销申请',
  `node_status` VARCHAR(4) DEFAULT NULL COMMENT '节点后状态：0=待审批 1=已通过 2=已驳回 3=已取消',
  `operator_id` BIGINT DEFAULT NULL COMMENT '操作人用户ID（提交人/审批人）',
  `operator_name` VARCHAR(50) DEFAULT NULL COMMENT '操作人姓名',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注（审批意见等）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '节点时间',
  PRIMARY KEY (`id`),
  KEY `idx_biz` (`biz_type`, `biz_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审批流程历史表';

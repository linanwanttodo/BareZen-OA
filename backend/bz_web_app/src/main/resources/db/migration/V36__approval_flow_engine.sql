-- ============================================
-- BareZen_Base V36: 多级审批引擎（流程模板 + 节点任务）
-- ============================================
-- 背景：V31 注释预告的「审批流模板表」方向落地。将单级「提交→审批」演进为
-- 模板驱动的多级节点链：approval_flow_template 定义每类业务单的审批链，
-- approval_flow_task 记录每条单据的节点任务（含审批人/意见/时间）。
-- approval_flow_log 继续保留为事件审计（追加式），职责不变。
--
-- 设计约定：
-- 1. 同一单据同一时刻至多一行 task.status='0'（当前待审节点），审批通过后
--    按模板推进式插入后续节点；末节点通过/任意节点驳回即终态。
-- 2. 模板种子默认每类 1 节点（any）= 兼容既有单级行为；管理员在模板中
--    追加 node_seq 更大的行即变为多级。
-- 3. 业务表 status 语义不变：0=审批中（多级期间保持） 1=通过 2=驳回 3=撤销。
-- 4. 指定审批人（assignee）仅作用于第一节点，来源于业务单 V31 assignee_id 字段。

-- 1. 审批流程模板表
CREATE TABLE IF NOT EXISTS `approval_flow_template` (
  `id` BIGINT NOT NULL COMMENT '主键ID（雪花ID）',
  `biz_type` VARCHAR(30) NOT NULL COMMENT '业务类型：leave=请假 overtime=加班 travel=出差 vehicle=用车',
  `node_seq` INT NOT NULL COMMENT '节点顺序（从1开始，不要求连续）',
  `node_name` VARCHAR(50) NOT NULL COMMENT '节点名称（如：部门主管审批 / HR审批）',
  `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：1=启用 0=停用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建者ID',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新者ID',
  `del_flag` TINYINT DEFAULT 0 COMMENT '逻辑删除：0=未删 1=已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_seq` (`biz_type`, `node_seq`, `del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审批流程模板表';

-- 2. 审批节点任务表
CREATE TABLE IF NOT EXISTS `approval_flow_task` (
  `id` BIGINT NOT NULL COMMENT '主键ID（雪花ID）',
  `biz_type` VARCHAR(30) NOT NULL COMMENT '业务类型：leave=请假 overtime=加班 travel=出差 vehicle=用车',
  `biz_id` BIGINT NOT NULL COMMENT '业务记录ID',
  `node_seq` INT NOT NULL COMMENT '节点顺序（对应模板）',
  `node_name` VARCHAR(50) NOT NULL COMMENT '节点名称',
  `status` VARCHAR(4) NOT NULL DEFAULT '0' COMMENT '节点状态：0=待审批 1=已通过 2=已驳回 3=已取消',
  `approver_id` BIGINT DEFAULT NULL COMMENT '审批人用户ID（NULL=任意有权限者）',
  `approver_name` VARCHAR(50) DEFAULT NULL COMMENT '审批人姓名',
  `approve_remark` VARCHAR(500) DEFAULT NULL COMMENT '审批意见',
  `approve_time` VARCHAR(30) DEFAULT NULL COMMENT '审批时间（yyyy-MM-dd HH:mm:ss）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建者ID',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新者ID',
  `del_flag` TINYINT DEFAULT 0 COMMENT '逻辑删除：0=未删 1=已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_node` (`biz_type`, `biz_id`, `node_seq`, `del_flag`),
  KEY `idx_biz` (`biz_type`, `biz_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审批节点任务表';

-- 3. 模板种子：4 类业务单各 1 级（any 规则）= 兼容既有单级审批行为
INSERT IGNORE INTO `approval_flow_template` (`id`, `biz_type`, `node_seq`, `node_name`, `enabled`) VALUES
(1, 'leave', 1, '请假审批', 1),
(2, 'overtime', 1, '加班审批', 1),
(3, 'travel', 1, '出差审批', 1),
(4, 'vehicle', 1, '用车审批', 1);
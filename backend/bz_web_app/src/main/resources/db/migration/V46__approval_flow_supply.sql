-- 审批引擎接入：用品申领（supply）启用单级审批模板
-- 使 5 类单据（请假/加班/出差/用车/用品申领）统一走 approval_flow_task 待审节点，
-- 待办中心可基于引擎节点做数据库分页（移除原有"业务表 status='0'"独立聚合）
-- 与 V36 四种业务模板同构：单级、any 规则，兼容既有单级审批行为
INSERT IGNORE INTO `approval_flow_template` (`id`, `biz_type`, `node_seq`, `node_name`, `enabled`) VALUES
(5, 'supply', 1, '用品申领审批', 1);
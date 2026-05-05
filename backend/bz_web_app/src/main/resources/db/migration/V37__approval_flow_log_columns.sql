-- ============================================
-- BareZen_Base V37: approval_flow_log 补齐公共审计列
-- ============================================
-- V22 建表时未包含 update_time/create_by/update_by/del_flag 四列，
-- 但 ApprovalFlowLog 实体继承 BaseEntity，MyBatis-Plus 查询会 SELECT 全列
-- （含 update_time 等），导致 flow-logs 接口 SQL 语法错误（Unknown column）。
-- 本迁移补齐四列，语义与 BaseEntity 一致（del_flag=0 未删除）。

ALTER TABLE `approval_flow_log`
  ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER `create_time`,
  ADD COLUMN `create_by` BIGINT DEFAULT NULL COMMENT '创建者用户ID' AFTER `update_time`,
  ADD COLUMN `update_by` BIGINT DEFAULT NULL COMMENT '更新者用户ID' AFTER `create_by`,
  ADD COLUMN `del_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0=未删除 1=已删除' AFTER `update_by`;
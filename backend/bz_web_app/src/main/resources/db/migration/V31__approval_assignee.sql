-- ============================================
-- BareZen_Base V31: 审批流程增强——指定审批人
-- ============================================
-- 背景：对应大型企业 OA「审批权责明确/审批流程灵活度」需求（市场调研中流程灵活度
-- 是选型最核心指标）。当前审批为「任意有审批权限者审批」，缺乏权责归属。
-- 方案（降级但真实可用）：提交申请时可选指定审批人（assignee_id/assignee_name），
-- 审批时仅指定审批人可操作（未指定则维持原「有权限者审批」逻辑）。
-- 多级审批/条件分支需引入审批流模板表，属大工程，列为后续路线图。

-- 1. 请假申请：指定审批人
ALTER TABLE `att_leave_request`
  ADD COLUMN `assignee_id` BIGINT DEFAULT NULL COMMENT '指定审批人用户ID（NULL=任意有权限者审批）' AFTER `approve_remark`,
  ADD COLUMN `assignee_name` VARCHAR(50) DEFAULT NULL COMMENT '指定审批人姓名' AFTER `assignee_id`;

-- 2. 加班申请：指定审批人
ALTER TABLE `att_overtime_request`
  ADD COLUMN `assignee_id` BIGINT DEFAULT NULL COMMENT '指定审批人用户ID（NULL=任意有权限者审批）' AFTER `approve_remark`,
  ADD COLUMN `assignee_name` VARCHAR(50) DEFAULT NULL COMMENT '指定审批人姓名' AFTER `assignee_id`;

-- 3. 出差申请：指定审批人
ALTER TABLE `att_travel_request`
  ADD COLUMN `assignee_id` BIGINT DEFAULT NULL COMMENT '指定审批人用户ID（NULL=任意有权限者审批）' AFTER `approve_remark`,
  ADD COLUMN `assignee_name` VARCHAR(50) DEFAULT NULL COMMENT '指定审批人姓名' AFTER `assignee_id`;

-- 4. 用车申请：指定审批人
ALTER TABLE `office_vehicle_request`
  ADD COLUMN `assignee_id` BIGINT DEFAULT NULL COMMENT '指定审批人用户ID（NULL=任意有权限者审批）' AFTER `approve_remark`,
  ADD COLUMN `assignee_name` VARCHAR(50) DEFAULT NULL COMMENT '指定审批人姓名' AFTER `assignee_id`;

-- 5. 用品申领：指定审批人
ALTER TABLE `office_supply_request`
  ADD COLUMN `assignee_id` BIGINT DEFAULT NULL COMMENT '指定审批人用户ID（NULL=任意有权限者审批）' AFTER `approve_remark`,
  ADD COLUMN `assignee_name` VARCHAR(50) DEFAULT NULL COMMENT '指定审批人姓名' AFTER `assignee_id`;

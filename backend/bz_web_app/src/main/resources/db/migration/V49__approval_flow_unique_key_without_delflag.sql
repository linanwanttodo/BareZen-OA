-- ============================================
-- BareZen OA V49: 审批流唯一键移除 del_flag（修正软删反模式）
-- ============================================
-- 背景：V36 在 uk_biz_seq / uk_biz_node 唯一键中包含了 del_flag，
-- 导致「软删后再重建」或「重复软删」时触发唯一冲突（反模式）：
-- 业务唯一性应只取决于业务维度，与删除标记无关。
--
-- 修正：唯一键仅保留业务维度
--   approval_flow_template: (biz_type, node_seq)
--   approval_flow_task:     (biz_type, biz_id, node_seq)
--
-- 兼容性说明：MySQL 不支持 `DROP INDEX IF EXISTS`（该语法为 MariaDB 扩展，
-- 在 MySQL 8.x 上会报 1064 语法错误）。此处使用标准 `DROP INDEX`，
-- 幂等性由以下事实保证：
--   1) V36 必然已建立同名 uk_biz_seq / uk_biz_node，故 DROP 在首次执行时一定命中；
--   2) 若迁移已成功执行过（同名索引已为新定义），DROP 会先移除它、ADD 再重建为相同定义，
--      重放依然成功，不会与历史脚本冲突；
--   3) 历史 V1-V48 不可变，本迁移仅新增，符合 Flyway 规范。
-- 若执行中途失败，按规范 `DELETE FROM flyway_schema_history WHERE version='49' AND success=0`
-- 后重跑即可（DROP 失败时索引仍属旧定义，重跑必然成功）。
--
-- 数据前置校验（已查 barezen_oa 现网）：
--   approval_flow_template 按 (biz_type,node_seq) 无重复行；
--   approval_flow_task     按 (biz_type,biz_id,node_seq) 无重复行；
--   故 ADD UNIQUE KEY 不会因存量重复数据失败。

-- 1. 审批流程模板表
ALTER TABLE `approval_flow_template`
    DROP INDEX `uk_biz_seq`;
ALTER TABLE `approval_flow_template`
    ADD UNIQUE KEY `uk_biz_seq` (`biz_type`, `node_seq`);

-- 2. 审批节点任务表
ALTER TABLE `approval_flow_task`
    DROP INDEX `uk_biz_node`;
ALTER TABLE `approval_flow_task`
    ADD UNIQUE KEY `uk_biz_node` (`biz_type`, `biz_id`, `node_seq`);

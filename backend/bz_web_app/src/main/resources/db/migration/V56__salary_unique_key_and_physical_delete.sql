-- ============================================
-- BareZen OA V56: 修复 salary 唯一键与逻辑删除的冲突
-- ============================================
-- 背景：两表唯一键均以业务维度为准（与 V49 的取向一致，业务唯一性只看业务维度），
--       但删除语义此前按表错配：
--   salary_structure  uk_user_id (user_id)：
--       实体误标 @TableLogic，removeById 实际执行 UPDATE del_flag=1，user_id 被墓碑行
--       永久占用，员工工资结构删除后无法重建（管理端保存直接撞唯一键）。
--       配置数据无审计价值，实体已改为物理删除（去 @TableLogic），本迁移清理历史墓碑。
--   salary_payment_record  uk_user_month (user_id, payment_month)：
--       实体正确保留 @TableLogic（发放记录有审计价值），但唯一键不含 del_flag：
--       删除未发放草稿后，同月重新生成会撞墓碑行的唯一键。
--       迁移把唯一键扩为 (user_id, payment_month, del_flag)，删除后同月可重新生成。
--
-- 已知边界（del_flag 入键的反模式代价，V34/V39/V41 同款）：
--   同一 (user_id, payment_month) 至多存在一个墓碑行。「删除→重新生成→再删除」时
--   第二次软删会撞唯一键报错；已发放记录本就禁止删除（SalaryPaymentController 校验），
--   未发放草稿删除为低频操作，接受该边界。
--
-- 存量数据校验（ADD UNIQUE 不会因重复数据失败的原因）：
--   uk_user_month 现存约束保证每个 (user_id, payment_month) 至多一行，
--   故 (user_id, payment_month, del_flag) 视角下不可能出现重复行。
--
-- 幂等性：
--   1) DELETE 天然幂等，重放删不到任何行；
--   2) MySQL 8 不支持 DROP INDEX IF EXISTS，按 V55 惯例用 information_schema
--      判断唯一键定义是否已含 del_flag，已迁移则跳过 DDL，可安全重放；
--   3) 若执行中途失败，按规范 DELETE FROM flyway_schema_history
--      WHERE version='56' AND success=0 后重跑。

-- 1. 清理 salary_structure 历史软删墓碑，释放被占用的 user_id（实体已改物理删除）
DELETE FROM salary_structure WHERE del_flag = 1;

-- 2. salary_payment_record 唯一键扩入 del_flag（仅当尚未迁移时执行）
SET @uk_has_delflag := (
    SELECT COUNT(DISTINCT COLUMN_NAME) = 3
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'salary_payment_record'
      AND INDEX_NAME = 'uk_user_month'
      AND COLUMN_NAME IN ('user_id', 'payment_month', 'del_flag')
);

SET @ddl := IF(@uk_has_delflag > 0,
    'SELECT 1',
    'ALTER TABLE `salary_payment_record` DROP INDEX `uk_user_month`, ADD UNIQUE KEY `uk_user_month` (`user_id`, `payment_month`, `del_flag`)');

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

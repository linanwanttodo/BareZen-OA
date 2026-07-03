-- ============================================
-- BareZen_Base V53: 补齐「任务日志」菜单 + 按钮级授权
-- ============================================
-- 背景（前端功能缺口）：
--   后端 SysJobLogController（@Tag "任务日志"，/api/system/v1/job-logs/page）早已实现，
--   但前端既没有 api/job.ts 的 getJobLogPageApi，也没有对应页面与菜单入口，
--   导致「定时任务执行记录」这一运维能力在管理端完全不可达：
--   任务失败时无处可查，只能看后台日志文件。
--   本迁移补上菜单与授权，并保证 sys_menu id ≥ 10000（1-21 已被 V3/V6 占用）。
--
-- 幂等性：菜单用 INSERT IGNORE；授权用 INSERT IGNORE + SELECT 限定 del_flag=0，可安全重放。
-- path 约定：子菜单 path 必须含父前缀（monitor/job-log），不能写相对短 path
--           —— vue-router 中以 / 开头的子路由是绝对路径，相对短 path 会被注册到顶层，
--              导航侧解析 /monitor/job-log，点击即 404。
-- 状态语义：status '0'=正常 '1'=停用（RuoYi 约定，V48 已归一化存量）。

-- 1. 菜单：任务日志（挂在监控管理 id=8 之下，排在操作日志之后）
INSERT IGNORE INTO `sys_menu`
  (`id`, `parent_id`, `menu_name`, `menu_type`, `perms`, `path`, `component`, `icon`, `order_num`, `visible`, `status`, `remark`)
VALUES
  (10280, 8, '任务日志', 'C', 'monitor:job:log', 'monitor/job-log', 'monitor/job-log/index', 'job', 6, '0', '0', '定时任务执行日志菜单');

-- 2. 按钮级权限：后端 SysJobLogController.list 标注 @RequirePermission("monitor:job:list")，
--    与定时任务列表同串，故复用该权限位（不另造不存在的权限）。
--    当前 Controller 只提供分页查询，故只声明查询一条；后续若加清理接口再补 F 型行。
INSERT IGNORE INTO `sys_menu`
  (`id`, `parent_id`, `menu_name`, `menu_type`, `perms`, `path`, `component`, `icon`, `order_num`, `visible`, `status`, `remark`)
VALUES
  (10281, 10280, '任务日志查询', 'F', 'monitor:job:list', NULL, NULL, NULL, 1, '0', '0', '查询任务执行日志');

-- 3. 授权给超级管理员（role_id=1）—— 沿用 V52 的写法：只对库中确实存在且未删除的行生效
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, m.`id` FROM `sys_menu` m
WHERE m.`del_flag` = 0
  AND m.`id` IN (10280, 10281);

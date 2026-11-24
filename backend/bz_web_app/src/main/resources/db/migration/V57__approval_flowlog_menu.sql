-- ============================================
-- BareZen_Base V57: 「审批流水」全局查询菜单
-- ============================================
-- 背景（前端功能缺口）：
--   后端 ApprovalFlowLogController（/api/system/v1/flow-logs + /flow-tasks，
--   @RequirePermission("system:flowlog:list")）早已实现，但仅在各业务单详情
--   弹窗里按单号调用；管理员没有一个全局入口按「业务类型 + 申请单号」查询
--   审批进度与事件流水，只能逐个业务单点开看。
--   本迁移补上菜单入口（页面 views/approval/flowlog/index.vue）。
--
-- 幂等性：菜单用 INSERT IGNORE；授权沿用 V52/V53 写法，只对库中确实存在且
--         未删除的行生效，可安全重放。
-- path 约定：子菜单 path 含父前缀（monitor/flow-log），见 V51/V53 注释
--           —— vue-router 中以 / 开头的子路由是绝对路径，相对短 path 会被
--              注册到顶层，导航侧点击即 404。
-- 权限说明：接口权限 system:flowlog:list 已存在（V27 的 F 行 10246，挂待办
--           中心下，role 1 已授权），故不再新增 F 按钮行，避免同串权限重复
--           登记；C 菜单自身也携带该 perms，作为路由 meta.permissions 供
--           前端守卫校验。
-- 状态语义：status '0'=正常 '1'=停用（V48 已归一化）。

-- 1. 菜单：审批流水（挂在监控管理 id=8 之下，排在任务日志之后）
INSERT IGNORE INTO `sys_menu`
  (`id`, `parent_id`, `menu_name`, `menu_type`, `perms`, `path`, `component`, `icon`, `order_num`, `visible`, `status`, `remark`)
VALUES
  (10282, 8, '审批流水', 'C', 'system:flowlog:list', 'monitor/flow-log', 'approval/flowlog/index', 'file-text', 7, '0', '0', '审批流水全局查询菜单（按业务类型+申请单号查询节点链与事件流水）');

-- 2. 授权给超级管理员（role_id=1）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, m.`id` FROM `sys_menu` m
WHERE m.`del_flag` = 0
  AND m.`id` = 10282;

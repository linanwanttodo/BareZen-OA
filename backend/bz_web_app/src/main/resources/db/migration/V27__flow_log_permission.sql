-- ============================================
-- BareZen_Base V27: 审批流程日志接口补权限校验
-- ============================================
-- 背景：GET /api/system/v1/flow-logs 无任何权限校验，任意登录用户可
-- 遍历 bizType/bizId 越权查看他人审批进度（IDOR，P0-4）。
-- 方案：后端接口加 @RequirePermission("system:flowlog:list")，
-- 同时登记 F 类型按钮权限（挂「待办中心」id=10009 下），
-- 便于管理员在角色授权界面按需分配（审批流日志跨 HR/行政多模块使用）。

-- 1. 新增「流程进度查看」按钮权限
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `perms`, `path`, `component`, `icon`, `order_num`, `visible`, `status`, `remark`) VALUES
(10246, 10009, '流程进度查看', 'F', 'system:flowlog:list', NULL, NULL, '#', 1, '0', '1', '审批流程日志查看按钮权限');

-- 2. admin 角色授权
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(1, 10246);

-- ============================================
-- BareZen_Base V26: 补全管理端入口（日程管理 + 申领管理拆分）
-- ============================================
-- 背景：1) V18 建了 hr_schedule 表但遗漏菜单，管理端无日程管理入口；
--       2) 办公用品（id=10013）页面内含「用品管理/申领记录」双 Tab，
--       菜单层看不出申领管理入口，用户端「物品申领」无对应管理入口。
-- 方案：新增「日程管理」（挂人事管理）+ 新增「申领管理」（挂行政办公，
-- 与用品管理共用页面组件，按路由 path 定位到申领记录 Tab）；原 10013
-- 更名为「用品管理」，申领审批按钮改挂申领管理菜单。

-- 1. 原「办公用品」菜单更名「用品管理」
UPDATE `sys_menu` SET `menu_name` = '用品管理' WHERE `id` = 10013 AND `menu_name` = '办公用品';

-- 2. 新增「日程管理」菜单（挂人事管理 id=10001 下，order 6）
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `perms`, `path`, `component`, `icon`, `order_num`, `visible`, `status`, `remark`) VALUES
(10014, 10001, '日程管理', 'C', 'hr:schedule:list', 'hr/schedule', 'hr/schedule/index', 'calendar', 6, '0', '1', '全员日程管理菜单');

-- 3. 新增「申领管理」菜单（挂行政办公 id=10006 下，order 4；与用品管理共用页面，按 path 定位 Tab）
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `perms`, `path`, `component`, `icon`, `order_num`, `visible`, `status`, `remark`) VALUES
(10015, 10006, '申领管理', 'C', 'office:supply:list', 'office/supply-request', 'office/supply/index', 'apps', 4, '0', '1', '办公用品申领管理菜单');

-- 4. 新增「日程删除」按钮权限（挂日程管理下）
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `perms`, `path`, `component`, `icon`, `order_num`, `visible`, `status`, `remark`) VALUES
(10245, 10014, '日程删除', 'F', 'hr:schedule:delete', NULL, NULL, '#', 1, '0', '1', '日程删除按钮');

-- 5. 「申领审批」按钮改挂申领管理菜单（仅调整挂载关系，权限串不变）
UPDATE `sys_menu` SET `parent_id` = 10015 WHERE `id` = 10244 AND `parent_id` = 10013;

-- 6. admin 角色授权
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(1, 10014),
(1, 10245),
(1, 10015);
